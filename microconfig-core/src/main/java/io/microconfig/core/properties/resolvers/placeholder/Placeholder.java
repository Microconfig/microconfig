package io.microconfig.core.properties.resolvers.placeholder;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.PlaceholderResolveStrategy;
import io.microconfig.core.properties.Property;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static lombok.AccessLevel.PACKAGE;

@Getter
@EqualsAndHashCode(exclude = "defaultValue")
@RequiredArgsConstructor(access = PACKAGE)
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    private final String configType;
    @With
    private final String component;
    @With
    private final String environment;
    private final String key;
    private final String defaultValue;

    public Property resolveUsing(PlaceholderResolveStrategy strategy) {
        return strategy.resolve(component, key, environment, configType)
                .orElseThrow(() -> new IllegalStateException("Can't resolve " + this));
    }

    public DeclaringComponent getReferencedComponent() {
        return new DeclaringComponentImpl(configType, component, environment);
    }

    public boolean isSelfReferenced() {
        return component.equals(SELF_REFERENCE);
    }

    public boolean referencedTo(DeclaringComponent c) {
        return component.equals(c.getComponent()) && environment.equals(c.getEnvironment());
    }

    public Placeholder overrideBy(DeclaringComponent c) {
        return withComponent(c.getComponent())
                .withEnvironment(c.getEnvironment());
    }

    @Override
    public String toString() {
        return "${" +
                component +
                "[" + environment + "]" +
                "@" +
                key +
                (defaultValue == null ? "" : ":" + defaultValue) +
                "}";
    }
}