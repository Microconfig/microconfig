package io.microconfig.core.service.io.ioservice;


import io.microconfig.core.domain.Property;

import java.util.List;
import java.util.Map;

public interface ConfigReader {
    List<Property> properties(String env);

    Map<String, String> propertiesAsMap();

    Map<String, String> escapeResolvedPropertiesAsMap();

    Map<Integer, String> commentsByLineNumber();
}
