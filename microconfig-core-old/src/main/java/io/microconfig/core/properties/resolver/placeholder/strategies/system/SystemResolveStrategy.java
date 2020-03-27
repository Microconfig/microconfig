package io.microconfig.core.properties.resolver.placeholder.strategies.system;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.placeholder.PlaceholderResolveStrategy;
import io.microconfig.core.properties.sources.SpecialSource;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

import static io.microconfig.core.properties.Property.tempProperty;
import static io.microconfig.core.properties.sources.SpecialSource.ENV_OS_SOURCE;
import static io.microconfig.core.properties.sources.SpecialSource.SYSTEM_SOURCE;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SystemResolveStrategy implements PlaceholderResolveStrategy {
    private final String name;
    private final Function<String, ?> keyToValue;

    public static PlaceholderResolveStrategy systemPropertiesResolveStrategy() {
        return new SystemResolveStrategy(SYSTEM_SOURCE, System::getProperty);
    }

    public static PlaceholderResolveStrategy envVariablesResolveStrategy() {
        return new SystemResolveStrategy(ENV_OS_SOURCE, System::getenv);
    }

    @Override
    public Optional<Property> resolve(Component component, String propertyKey, String environment) {
        if (!name.equals(component.getName())) return empty();

        return ofNullable(keyToValue.apply(propertyKey))
                .map(Object::toString)
                .map(value -> tempProperty(propertyKey, value, environment, new SpecialSource(component, name)))
                .map(Property::escapeOnWindows);
    }
}