package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import io.microconfig.core.properties.io.tree.ComponentTree;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.ComponentProperty;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class ComponentPropertiesFactory {
    private final ComponentTree componentTree;
    private final File destinationComponentDir;

    public Map<String, ComponentProperty> get() {
        return of(
                new ComponentNameProperty(),
                new ComponentConfigDirProperty(componentTree),
                new ResultDirProperty(destinationComponentDir),
                new ConfigRootDirProperty(componentTree.getRootDir())
        ).collect(toMap(ComponentProperty::key, identity()));
    }
}