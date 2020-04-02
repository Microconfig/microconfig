package io.microconfig.core.properties;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBasedComponentTest {
    File file = new File("root/components/comp/config.yaml");
    FileBasedComponent source = new FileBasedComponent(file, 0, true, "app", "dev");

    @Test
    void getters() {
        assertEquals(file, source.getSource());
        assertEquals(0, source.getLineNumber());
        assertTrue(source.isYaml());
    }

    @Test
    void factoryMethod() {
        assertEquals(source, fileSource(file, 0, true, "app", "dev"));
    }

    @Test
    void parentComponent() {
        assertEquals("comp", source.getComponent());
    }

    @Test
    void string() {
        assertEquals("../components/comp/config.yaml:1", source.toString());
    }
}