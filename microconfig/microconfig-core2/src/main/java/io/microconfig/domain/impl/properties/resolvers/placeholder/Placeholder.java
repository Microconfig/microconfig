package io.microconfig.domain.impl.properties.resolvers.placeholder;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PACKAGE;

@Getter
@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder {
    private static final String SELF_REFERENCE = "this";

    private final String configType;
    private final String component;
    private final String environment;
    private final String value;
    private final String defaultValue;

    @Override
    public String toString() {
        return "${" +
                configType + "::" +
                component +
                "[" + environment + "]" +
                "@" +
                value +
                (defaultValue == null ? "" : ":" + defaultValue) +
                "}";
    }
}