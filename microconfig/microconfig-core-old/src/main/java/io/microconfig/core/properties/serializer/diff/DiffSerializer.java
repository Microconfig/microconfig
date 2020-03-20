package io.microconfig.core.properties.serializer.diff;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ioservice.ConfigIoService;
import io.microconfig.core.properties.serializer.ConfigSerializer;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static io.microconfig.utils.FileUtils.delete;
import static io.microconfig.utils.Logger.error;
import static io.microconfig.utils.Logger.warn;
import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
public class DiffSerializer implements ConfigSerializer {
    private static final String DIFF_PREFIX = "diff-";

    private final ConfigSerializer delegate;
    private final ConfigIoService configIoService;

    @Override
    public Optional<File> serialize(String component, String env, Collection<Property> currentProperties) {
        File current = configDestination(component, env, currentProperties);
        File diffFile = diffFile(current);

        Map<String, String> oldProperties = readOldConfig(current);
        Optional<File> result = delegate.serialize(component, env, currentProperties);
        Map<String, String> diff = compare(oldProperties, currentProperties);

        if (!diff.isEmpty()) {
            warn("Stored " + diff.size() + " property changes to " + component + "/" + diffFile.getName());
            configIoService.writeTo(diffFile).write(diff);
        }

        return result;
    }

    private File diffFile(File destination) {
        File diffFile = new File(destination.getParent(), DIFF_PREFIX + destination.getName());
        delete(diffFile);
        return diffFile;
    }

    private Map<String, String> readOldConfig(File current) {
        try {
            return configIoService.read(current).propertiesAsMap();
        } catch (RuntimeException e) {
            error("Can't read previous config '" + current + "' for comparison: " + e.getMessage());
            return emptyMap();
        }
    }

    private Map<String, String> compare(Map<String, String> old, Collection<Property> current) {
        if (old.isEmpty()) return emptyMap();

        Map<String, String> result = new TreeMap<>();

        for (Property p : current) {
            if (p.isTemp()) continue;

            String oldValue = old.remove(p.getKey());
            if (oldValue == null) {
                markAdded(p.getKey(), p.getValue(), result);
            } else if (!linesEquals(p.getValue(), oldValue)) {
                markChanged(p.getKey(), oldValue, p.getValue(), result);
            }
        }

        old.forEach((k, oldValue) -> markRemoved(k, oldValue, result));
        return result;
    }

    private boolean linesEquals(String current, String old) {
        return current.trim()
                .equals(old.trim());
    }

    private void markAdded(String key, String value, Map<String, String> result) {
        result.put("+" + key, value);
    }

    private void markRemoved(String key, String value, Map<String, String> result) {
        result.put("-" + key, value);
    }

    private void markChanged(String key, String oldValue, String currentValue, Map<String, String> result) {
        result.put(key, oldValue + " -> " + currentValue);
    }

    @Override
    public File configDestination(String component, String env, Collection<Property> properties) {
        return delegate.configDestination(component, env, properties);
    }
}