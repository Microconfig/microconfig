package io.microconfig.core.properties.provider;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.tree.ComponentTree;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.core.environments.Component.byType;
import static io.microconfig.core.properties.io.tree.ConfigFileFilters.*;

@RequiredArgsConstructor
public class FileBasedConfigProvider implements ConfigProvider {
    private final Set<String> configExtensions;
    private final ComponentTree componentTree;
    private final ComponentParser componentParser;

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return collectProperties(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> collectProperties(Component component, String env, Set<Include> processedIncludes) {
        Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> collectProperties(filter, component, env, processedIncludes);

        Map<String, Property> basicProperties = collectProperties.apply(defaultConfig(configExtensions));
        Map<String, Property> envSharedProperties = collectProperties.apply(configForMultipleEnvironments(configExtensions, env));
        Map<String, Property> envSpecificProperties = collectProperties.apply(configForOneEnvironment(configExtensions, env));

        basicProperties.putAll(envSharedProperties);
        basicProperties.putAll(envSpecificProperties);
        return basicProperties;
    }

    private Map<String, Property> collectProperties(Predicate<File> filter, Component component, String env, Set<Include> processedIncludes) {
        Map<String, Property> propertyByKey = new HashMap<>();

        componentTree.getConfigFiles(component.getType(), filter)
                .map(file -> componentParser.parse(file, env))
                .forEach(c -> processComponent(c, propertyByKey, processedIncludes));

        return propertyByKey;
    }

    private void processComponent(ParsedComponent parsedComponent, Map<String, Property> destination, Set<Include> processedIncludes) {
        Map<String, Property> included = processIncludes(parsedComponent.getIncludes(), processedIncludes);
        Map<String, Property> original = parsedComponent.getPropertiesAsMas();

        destination.putAll(included);
        destination.putAll(original);
    }

    private Map<String, Property> processIncludes(List<Include> includes, Set<Include> processedIncludes) {
        Map<String, Property> result = new HashMap<>();

        for (Include include : includes) {
            if (!processedIncludes.add(include)) continue;

            Map<String, Property> included = collectProperties(byType(include.getComponent()), include.getEnv(), processedIncludes);
            result.putAll(included);
        }

        return result;
    }
}