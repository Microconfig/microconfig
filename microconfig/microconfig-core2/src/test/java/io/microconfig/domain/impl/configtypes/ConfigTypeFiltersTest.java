package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.*;
import static io.microconfig.domain.impl.configtypes.StandardConfigType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypeFiltersTest {
    private final List<ConfigType> types = new StandardConfigTypeRepository().getConfigTypes();

    @Test
    void selectEachConfigType() {
        assertEquals(
                types,
                eachConfigType().selectTypes(types)
        );
    }

    @Test
    void useProvidedTypes() {
        assertEquals(
                singletonList(DEPLOY),
                configType(DEPLOY).selectTypes(emptyList())
        );
    }

    @Test
    void selectByConfigName() {
        assertEquals(
                asList(APPLICATION, HELM),
                configTypeWithName("app", "helm").selectTypes(types)
        );
    }

    @Test
    void failOnUnsupportedName() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName("badName").selectTypes(types));
    }

    @Test
    void selectByFileExtension() {
        File application = new File("application.yaml");
        assertEquals(
                singletonList(APPLICATION),
                configTypeWithExtensionOf(application).selectTypes(types)
        );
    }

    @Test
    void failOnFileWithoutExtension() {
        File badFile = new File("application");
        assertThrows(IllegalArgumentException.class, () -> configTypeWithExtensionOf(badFile).selectTypes(types));
    }

}