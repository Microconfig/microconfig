package io.microconfig.core.properties.resolver.placeholder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Math.max;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@With(PRIVATE)
@RequiredArgsConstructor
public class PlaceholderBorders {
    private static final PlaceholderBorders NOT_VALID = new PlaceholderBorders(new StringBuilder());

    private final StringBuilder line;

    @Getter
    private final int startIndex;
    private final int configTypeEndIndex;
    private final int envIndex;
    private final int valueIndex;
    private final int defaultValueIndex;
    @Getter
    private final int endIndex;

    static PlaceholderBorders findBorders(CharSequence sequence) {
        StringBuilder line = sequence instanceof StringBuilder ? (StringBuilder) sequence : new StringBuilder(sequence);
        return new PlaceholderBorders(line).searchOpenSign();
    }

    private PlaceholderBorders(StringBuilder line) {
        this(line, -1, -1, -1, -1, -1, -1);
    }

    private PlaceholderBorders searchOpenSign() {
        int index = line.indexOf("${", startIndex);
        if (index >= 0) {
            return new PlaceholderBorders(line)
                    .withStartIndex(index)
                    .parseComponentName();
        }

        return NOT_VALID;
    }

    private PlaceholderBorders parseComponentName() {
        for (int i = max(startIndex + 2, configTypeEndIndex + 3); i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':' && i + 1 < line.length() && line.charAt(i + 1) == ':') {
                return withConfigTypeEndIndex(i - 1).parseComponentName();
            }
            if (c == '[') {
                return withEnvIndex(i + 1).parseEnvName();
            }
            if (c == '@') {
                return withValueIndex(i + 1).parseValue();
            }
            if (!isAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return NOT_VALID;
    }

    private PlaceholderBorders parseEnvName() {
        for (int i = envIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ']' && i + 1 < line.length() && line.charAt(i + 1) == '@') {
                return withValueIndex(i + 2).parseValue();
            }
            if (!isAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return NOT_VALID;
    }

    private PlaceholderBorders parseValue() {
        for (int i = valueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':') {
                return withDefaultValueIndex(i + 1).parseDefaultValue();
            }
            if (c == '}') {
                return withEndIndex(i);
            }
            if (!isAllowedSymbol(c) && c != '/' && c != '\\') {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return NOT_VALID;
    }

    private PlaceholderBorders parseDefaultValue() {
        int closeBracketLastIndex = -1;
        int openBrackets = 1;
        for (int i = defaultValueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == '{') {
                char prevChar = line.charAt(i - 1);
                if (prevChar == '$' || prevChar == '#') {
                    ++openBrackets;
                }
                continue;
            }
            if (c == '}') {
                closeBracketLastIndex = i;
                if (--openBrackets == 0) {
                    return withEndIndex(closeBracketLastIndex);
                }
            }
        }

        return closeBracketLastIndex < 0 ? NOT_VALID : withEndIndex(closeBracketLastIndex);
    }

    private boolean isAllowedSymbol(char c) {
        return isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }

    public Placeholder toPlaceholder(String contextEnv) {
        return new Placeholder(
                ofNullable(configTypeEndIndex < 0 ? null : line.substring(startIndex + 2, configTypeEndIndex + 1)),
                line.substring(max(startIndex + 2, configTypeEndIndex + 3), envIndex < 0 ? valueIndex - 1 : envIndex - 1),
                envIndex < 0 ? contextEnv : line.subSequence(envIndex, valueIndex - 2).toString(),
                line.substring(valueIndex, defaultValueIndex < 0 ? endIndex : defaultValueIndex - 1),
                ofNullable(defaultValueIndex < 0 ? null : line.substring(defaultValueIndex, endIndex))
        );
    }

    public boolean isValid() {
        return startIndex >= 0;
    }

    @Override
    public String toString() {
        return line.substring(startIndex, endIndex + 1);
    }
}