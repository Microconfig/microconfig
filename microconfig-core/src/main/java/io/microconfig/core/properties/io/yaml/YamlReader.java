package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.FileBasedComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.AbstractConfigReader;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.OverrideProperty.isOverrideProperty;
import static io.microconfig.core.properties.OverrideProperty.overrideProperty;
import static io.microconfig.core.properties.PropertyImpl.isComment;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.isBlank;
import static java.lang.Character.isWhitespace;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

class YamlReader extends AbstractConfigReader {
    private final Deque<KeyOffset> currentProperty = new ArrayDeque<>();

    YamlReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
    }

    @Override
    public List<Property> properties(String configType, String environment) {
        List<Property> result = new ArrayList<>();

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);
            String multiLineKey = multiLineKey(line, currentOffset);
            if (multiLineKey != null) {
                lineNumber = multiLineValue(result, multiLineKey, lineNumber, currentOffset + 2, configType, environment);
            } else if (isMultiValue(line, currentOffset)) {
                lineNumber = addMultiValue(result, currentOffset, lineNumber, configType, environment);
            } else {
                parseSimpleProperty(result, currentOffset, lineNumber, configType, environment);
            }
        }

        return result;
    }

    private String multiLineKey(String line, int currentOffset) {
        int separatorIndex = separatorIndex(line, currentOffset);
        if (separatorIndex < 0 || separatorIndex == line.length() - 1) return null;
        boolean multilinePostfix = line.chars().skip(separatorIndex + 1).allMatch(c -> isWhitespace(c) || c == '|');
        return multilinePostfix ? line.substring(0, separatorIndex) : null;
    }

    private int multiLineValue(List<Property> result, String key, int index, int offset, String configType, String env) {
        List<String> valueLines = new ArrayList<>();
        int counter = 1;
        while (true) {
            if (index + counter >= lines.size()) break;
            String line = lines.get(index + counter);
            int currentOffset = line.isEmpty() ? 0 : offsetIndex(line);
            if (currentOffset < offset) break;
            String value = line.substring(offset);
            if (!value.trim().isEmpty()) valueLines.add(line.substring(offset));
            counter++;
        }
        if (valueLines.isEmpty()) throw new IllegalArgumentException("Missing value in multiline key '" + key + "' in '"
                + new FileBasedComponent(file, index, true, configType, env) + "'");
        FileBasedComponent source = fileSource(file, index, true, configType, env);
        String k = mergeKey(key);
        String v = join(LINES_SEPARATOR, valueLines);
        Property p = isOverrideProperty(k) ? overrideProperty(k, v, YAML, source) : property(k, v, YAML, source);
        result.add(p);
        return index + counter - 1;
    }

    private String mergeKey(String key) {
        return Stream.concat(
                        currentProperty.stream().map(KeyOffset::toString),
                        Stream.of(key.trim())
                )
                .collect(joining("."));
    }

    private boolean isMultiValue(String line, int currentOffset) {
        char c = line.charAt(currentOffset);
        return asList('-', '[', ']', '{').contains(c) ||
                (c == '$' && line.length() > currentOffset + 1 && line.charAt(currentOffset + 1) == '{');
    }

    private int addMultiValue(List<Property> result,
                              int currentOffset,
                              int originalLineNumber, String configType, String env) {
        StringBuilder value = new StringBuilder();
        int index = originalLineNumber;
        while (true) {
            String line = lines.get(index);
            if (!line.isEmpty()) {
                value.append(line.substring(currentOffset));
            }
            if (index + 1 >= lines.size()) {
                break;
            }
            if (multilineValueEnd(lines.get(index + 1), currentOffset)) {
                break;
            }

            value.append(LINES_SEPARATOR);
            ++index;
        }

        addValue(result, currentOffset, originalLineNumber, null, value.toString(), configType, env);
        return index;
    }

    private boolean multilineValueEnd(String nextLine, int currentOffset) {
        if (skip(nextLine) && !isComment(nextLine)) return false;

        int nextOffset = offsetIndex(nextLine);
        if (currentOffset > nextOffset) return true;
        return currentOffset == nextOffset && !isMultiValue(nextLine, nextOffset);
    }

    private void parseSimpleProperty(List<Property> result,
                                     int currentOffset,
                                     int index, String configType, String env) {
        String line = lines.get(index);
        int separatorIndex = separatorIndex(line, currentOffset);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect delimiter in '" + line + "' in '" + new FileBasedComponent(file, index, true, configType, env) +
                    "'\nYaml property must contain ':' as delimiter.");
        }

        removePropertiesWithBiggerOffset(currentOffset);

        String key = line.substring(currentOffset, separatorIndex).trim();

        if (valueEmpty(line, separatorIndex)) {
            if (itsLastProperty(index, currentOffset)) {
                addValue(result, currentOffset, index - 1, key, "", configType, env);
                return;
            }

            currentProperty.add(new KeyOffset(key, currentOffset, index));
            return;
        }

        String value = line.substring(separatorIndex + 1).trim();
        addValue(result, currentOffset, index, key, value, configType, env);
    }

    private int separatorIndex(String line, int offset) {
        return line.indexOf(':', offset);
    }

    private boolean valueEmpty(String line, int separatorIndex) {
        return isBlank(line.substring(separatorIndex + 1));
    }

    private void removePropertiesWithBiggerOffset(int currentOffset) {
        while (!currentProperty.isEmpty() && currentProperty.peekLast().offset >= currentOffset) {
            currentProperty.pollLast();
        }
    }

    private boolean skip(String line) {
        String trim = line.trim();
        return trim.isEmpty() || isComment(trim);
    }

    private int offsetIndex(String line) {
        return range(0, line.length())
                .filter(i -> !isWhitespace(line.charAt(i)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("assertion error: line is empty"));
    }

    private boolean itsLastProperty(int i, int currentOffset) {
        ++i;
        while (i < lines.size()) {
            String line = lines.get(i++);
            if (skip(line)) continue;
            int offsetIndex = offsetIndex(line);
            if (currentOffset > offsetIndex) {
                return true;
            }
            if (currentOffset == offsetIndex) {
                return !isMultiValue(line, offsetIndex);
            }
            return false;
        }

        return true;
    }

    private void addValue(List<Property> result,
                          int currentOffset, int line,
                          String lastKey, String value, String configType, String env) {
        if (lastKey != null) {
            currentProperty.add(new KeyOffset(lastKey, currentOffset, line));
        }
        int lineNumber = currentProperty.peekLast().lineNumber;
        String key = toProperty();
        currentProperty.pollLast();
        FileBasedComponent source = fileSource(file, lineNumber, true, configType, env);
        Property prop = isOverrideProperty(key) ? overrideProperty(key, value, YAML, source) : property(key, value, YAML, source);
        result.add(prop);
    }

    private String toProperty() {
        return currentProperty.stream()
                .map(k -> k.key)
                .collect(joining("."));
    }

    @RequiredArgsConstructor
    private static class KeyOffset {
        private final String key;
        private final int offset;
        private final int lineNumber;

        @Override
        public String toString() {
            return key;
        }
    }
}