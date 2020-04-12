package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.ComponentFactory;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentImpl;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.io.FsReader;
import io.microconfig.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.getName;
import static io.microconfig.utils.FileUtils.walk;
import static io.microconfig.utils.StreamUtils.filter;
import static io.microconfig.utils.StreamUtils.forEach;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class FileEnvironmentRepository implements EnvironmentRepository {
    public static final String ENV_DIR = "envs";

    private final File envDir;
    private final FsReader fsReader;
    private final ComponentFactory componentFactory;
    private final PropertiesFactory propertiesFactory;

    public FileEnvironmentRepository(File rootDir, FsReader fsReader,
                                     ComponentFactory componentFactory, PropertiesFactory propertiesFactory) {
        this.envDir = new File(rootDir, ENV_DIR);
        this.propertiesFactory = propertiesFactory;
        if (!envDir.exists()) {
            throw new IllegalArgumentException("Env directory doesn't exist: " + envDir);
        }
        this.fsReader = fsReader;
        this.componentFactory = componentFactory;
    }

    @Override
    public List<Environment> environments() {
        return forEach(environmentFiles(), parse());
    }

    @Override
    public Set<String> environmentNames() {
        return forEach(environmentFiles(), FileUtils::getName, toCollection(TreeSet::new));
    }

    @Override
    public Environment getByName(String name) {
        return findEnvWith(name).orElseThrow(notFoundException(name));
    }

    @Override
    public Environment getOrCreateByName(String name) {
        return findEnvWith(name).orElseGet(fakeEnvWith(name));
    }

    private Optional<Environment> findEnvWith(String name) {
        return envFileWith(name).map(parse());
    }

    private Optional<File> envFileWith(String name) {
        List<File> envFiles = filter(environmentFiles(), withFileName(name));
        if (envFiles.size() > 1) {
            throw new EnvironmentException("Found several env files with name: " + name);
        }
        return envFiles.isEmpty() ? empty() : of(envFiles.get(0));
    }

    private List<File> environmentFiles() {
        try (Stream<Path> stream = walk(envDir.toPath())) {
            return stream.map(Path::toFile)
                    .filter(hasSupportedExtension())
                    .collect(toList());
        }
    }

    private Function<File, Environment> parse() {
        return parseDefinition().andThen(def -> def.toEnvironment(componentFactory, propertiesFactory));
    }

    private Function<File, EnvironmentDefinition> parseDefinition() {
        return f -> new EnvironmentFile(f)
                .parseUsing(fsReader)
                .processIncludeUsing(definitionRepository())
                .checkComponentNamesAreUnique();
    }

    private Function<String, EnvironmentDefinition> definitionRepository() {
        return name -> envFileWith(name)
                .map(parseDefinition())
                .orElseThrow(notFoundException(name));
    }

    private Predicate<File> hasSupportedExtension() {
        return f -> {
            String name = f.getName();
            return name.endsWith(".yaml") || name.endsWith(".json");
        };
    }

    private Predicate<File> withFileName(String envName) {
        return f -> getName(f).equals(envName);
    }

    private Supplier<EnvironmentException> notFoundException(String name) {
        return () -> new EnvironmentException("Can't find env '" + name + "'. Available envs: " + environmentNames());
    }

    private Supplier<Environment> fakeEnvWith(String name) {
        return () -> new EnvironmentImpl(null, name, 0, emptyList(), componentFactory, propertiesFactory);
    }
}
