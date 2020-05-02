package io.microconfig.core.properties.io.properties;

import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesConfigIoTest {
    PropertiesConfigIo ioService = new PropertiesConfigIo(new DumpedFsReader());

    @Test
    void test() {
        File file = classpathFile("configFormats/properties/propLine.properties");
        assertEquals(expectedMap(), ioService.readFrom(file).propertiesAsMap());
    }

    private Map<String, String> expectedMap() {
        String multilineEscape = "\\" + LINES_SEPARATOR;
        Map<String, String> expected = new TreeMap<>();
        expected.put("p", "p_v");
        expected.put("p2", "p2_v");
        expected.put("p3", "=p3_v");
        expected.put("p4", ":p4_v");
        expected.put("jwt.publicKey", "-----BEGIN PUBLIC KEY-----" + multilineEscape +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgfjVb7pJlEdu9lDPOxmi" + multilineEscape +
                "LwIDAQAB" + multilineEscape +
                "-----END PUBLIC KEY-----");
        expected.put("empty", "");
        expected.put("empty2", "");
        return expected;
    }
}