package io.microconfig.io;

import io.microconfig.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DumpedFsReader implements FsReader {
    @Override
    public String readFully(File file) {
        return IoUtils.readFully(file);
    }

    @Override
    public List<String> readLines(File file) {
        return IoUtils.readLines(file);
    }

    @Override
    public Optional<String> firstLineOf(File file, Predicate<String> predicate) {
        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.filter(predicate).findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}