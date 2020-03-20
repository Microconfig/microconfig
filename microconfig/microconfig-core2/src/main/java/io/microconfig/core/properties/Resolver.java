package io.microconfig.core.properties;

import io.microconfig.core.configtypes.ConfigType;

public interface Resolver {
    String resolve(CharSequence value, String envContext, ConfigType configType);
}