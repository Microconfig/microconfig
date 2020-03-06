package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentDoesNotExistException;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.utils.reader.FilesReader;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static io.microconfig.utils.FileUtils.walk;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileBasedEnvironmentProvider implements EnvironmentProvider {
    private final File envDir;
    private final EnvironmentParserSelector environmentParserSelector;
    private final FilesReader fileReader;

    public FileBasedEnvironmentProvider(File envDir, EnvironmentParserSelector environmentParserSelector, FilesReader fileReader) {
        this.envDir = envDir;
        this.environmentParserSelector = environmentParserSelector;
        this.fileReader = fileReader;

        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist " + envDir);
        }
    }

    @Override
    public Set<String> getEnvironmentNames() {
        try (Stream<File> envStream = envFiles(null)) {
            return envStream
                    .map(f -> f.getName().substring(0, f.getName().indexOf('.')))
                    .collect(toCollection(TreeSet::new));
        }
    }

    @Override
    public Environment getByName(String name) {
        File envFile = findEnvFile(name);

        return environmentParserSelector.selectParser(envFile)
                .parse(name, fileReader.read(envFile))
                .withSource(envFile)
                .processInclude(this)
                .verifyUniqueComponentNames();
    }

    private File findEnvFile(String name) {
        List<File> files = getEnvFiles(name);

        if (files.size() > 1) {
            throw new IllegalArgumentException("Found several env files with name " + name);
        }
        if (files.isEmpty()) {
            throw new EnvironmentDoesNotExistException("Can't find env with name " + name);
        }
        return singleValue(files);
    }

    private List<File> getEnvFiles(String name) {
        try (Stream<File> envStream = envFiles(name)) {
            return envStream.collect(toList());
        }
    }

    private Stream<File> envFiles(String envName) {
        List<String> supportedFormats = environmentParserSelector.supportedFormats();
        Predicate<File> fileNamePredicate = envName == null ?
                f -> supportedFormats.stream().anyMatch(format -> f.getName().endsWith(format)) :
                f -> supportedFormats.stream().anyMatch(format -> f.getName().equals(envName + format));

        return walk(envDir.toPath())
                .map(Path::toFile)
                .filter(fileNamePredicate);
    }
}
