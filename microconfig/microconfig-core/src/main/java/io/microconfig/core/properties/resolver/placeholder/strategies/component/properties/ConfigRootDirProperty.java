package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.resolver.placeholder.strategies.component.ComponentProperty;

import java.io.File;
import java.util.Optional;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.Optional.of;

public class ConfigRootDirProperty implements ComponentProperty {
    private final String configRoot;

    public ConfigRootDirProperty(File configRoot) {
        this.configRoot = unixLikePath(configRoot.getAbsolutePath());
    }

    @Override
    public String key() {
        return "configRoot";
    }

    @Override
    public Optional<String> value(Component ignore) {
        return of(configRoot);
    }
}