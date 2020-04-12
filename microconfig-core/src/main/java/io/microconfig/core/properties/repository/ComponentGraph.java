package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface ComponentGraph {
    List<ConfigFile> getConfigFilesOf(String component, String environment, ConfigType configType);

    Optional<File> getFolderOf(String component);
}