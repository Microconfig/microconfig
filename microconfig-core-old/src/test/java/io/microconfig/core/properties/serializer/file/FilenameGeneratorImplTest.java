package io.microconfig.core.properties.serializer.file;

import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.core.properties.Property.property;
import static io.microconfig.core.properties.sources.FileSource.fileSource;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FilenameGeneratorImplTest {
    private FilenameGeneratorImpl generator = new FilenameGeneratorImpl(new File("components"), ".mgmt", "application");

    @Test
    void testFileFor() {
        doTest(true);
        doTest(false);
    }

    private void doTest(boolean yaml) {
        String ext = yaml ? "yaml" : "properties";

        List<Property> properties = singletonList(property("key", "value", "env", fileSource(new File("c1/application." + ext), 0, yaml)));
        File result = generator.fileFor("c1", "env", properties);
        assertEquals(new File("components/c1/.mgmt/application." + ext), result);
    }
}