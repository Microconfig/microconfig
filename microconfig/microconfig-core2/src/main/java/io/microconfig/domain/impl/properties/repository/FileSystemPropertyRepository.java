package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyRepository;
import io.microconfig.io.fsgraph.ComponentNotFoundException;
import io.microconfig.io.fsgraph.FileSystemGraph;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.io.fsgraph.ConfigFileFilters.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class FileSystemPropertyRepository implements PropertyRepository {
    private final FileSystemGraph fsGraph;
    private final ConfigDefinitionParser configDefinitionParser;

    @Override
    public List<Property> getProperties(String componentType, String environment, ConfigType configType) {
        return new ComponentConfig(componentType, environment, configType)
                .getProperties()
                .values().stream()
                .sorted(comparing(Property::getKey))
                .collect(toList());
    }

    @RequiredArgsConstructor
    private class ComponentConfig {
        @With
        private final String componentType;
        @With
        private final String environment;

        private final Set<String> configExtensions;
        private final Set<Include> processedIncludes;

        public ComponentConfig(String componentType, String environment, ConfigType configType) {
            this(componentType, environment, configType.getSourceExtensions(), new LinkedHashSet<>());
        }

        public Map<String, Property> getProperties() {
            Map<String, Property> basicProperties = filter(defaultConfig(configExtensions));
            Map<String, Property> envSharedProperties = filter(configForMultipleEnvironments(configExtensions, environment));
            Map<String, Property> envSpecificProperties = filter(configForOneEnvironment(configExtensions, environment));

            basicProperties.putAll(envSharedProperties);
            basicProperties.putAll(envSpecificProperties);
            return basicProperties;
        }

        private Map<String, Property> filter(Predicate<File> configFilter) {
            try {
                return collectPropertiesFrom(configDefinitionsFor(configFilter));
            } catch (ComponentNotFoundException e) {
                throw e.withComponentParent(componentType);
            }
        }

        private Stream<ConfigDefinition> configDefinitionsFor(Predicate<File> filter) {
            return fsGraph.getConfigFilesFor(componentType, filter)
                    .map(file -> configDefinitionParser.parse(file, environment));
        }

        private Map<String, Property> collectPropertiesFrom(Stream<ConfigDefinition> componentConfigs) {
            Map<String, Property> componentProperties = new HashMap<>();

            componentConfigs.forEach(component -> {
                componentProperties.putAll(collectIncludedProperties(component.getIncludes()));
                componentProperties.putAll(component.getProperties());
            });

            return componentProperties;
        }

        private Map<String, Property> collectIncludedProperties(List<Include> includes) {
            return includes.stream()
                    .filter(processedIncludes::add)
                    .map(include -> includedComponent(include).getProperties())
                    .reduce(new HashMap<>(), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }

        private ComponentConfig includedComponent(Include include) {
            return withComponentType(include.getComponentType())
                    .withEnvironment(include.getEnvironment());
        }
    }
}
