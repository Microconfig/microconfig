package io.microconfig;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

@RequiredArgsConstructor
public class MicroconfigParams {
    private final CommandLineParamParser parser;

    public static MicroconfigParams parse(String... args) {
        return new MicroconfigParams(CommandLineParamParser.parse(args));
    }

    public File rootDir() {
        return new File(parser.valueOr("r", "."));
    }

    public File destinationDir() {
        return new File(parser.valueOr("d", "build"));
    }

    public String env() {
        return parser.requiredValue("e", "set -e (environment)");
    }

    public List<String> groups() {
        return parser.listValue("g");
    }

    public List<String> services() {
        return parser.listValue("s");
    }

    public boolean stacktrace() {
        return parser.booleanValue("stacktrace");
    }

    public boolean version() {
        return parser.contains("v");
    }

    public boolean jsonOutput() {
        return "json".equals(parser.value("output"));
    }
}