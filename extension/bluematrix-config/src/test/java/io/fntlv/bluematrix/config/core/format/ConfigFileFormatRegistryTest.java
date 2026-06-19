package io.fntlv.bluematrix.config.core.format;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileFormatRegistryTest {

    @Test
    void defaultRegistryContainsYamlAndJson() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        assertTrue(registry.getByName(ConfigFileFormats.YAML).isPresent());
        assertTrue(registry.getByName(ConfigFileFormats.JSON).isPresent());
        assertEquals(ConfigFileFormats.YAML, registry.getDefaultFileFormat().name());
    }

    @Test
    void extensionLookupNormalizesDotAndCase() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        assertTrue(registry.getByExtension(".yml").isPresent());
        assertTrue(registry.getByExtension("YML").isPresent());
    }

    @Test
    void duplicateNameFailsFast() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TestFileFormat(ConfigFileFormats.YAML, "test")));
    }

    @Test
    void duplicateExtensionFailsFast() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TestFileFormat("custom", "yml")));
    }

    @Test
    void getFileFormatsReturnsUnmodifiableCollection() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        assertThrows(UnsupportedOperationException.class,
                () -> registry.getFileFormats().clear());
    }

    @Test
    void setDefaultFileFormatChangesDefaultFileFormat() {
        ConfigFileFormatRegistry registry = new ConfigFileFormatRegistry();

        registry.setDefaultFileFormat(ConfigFileFormats.JSON);

        assertEquals(ConfigFileFormats.JSON, registry.getDefaultFileFormat().name());
    }

    private static class TestFileFormat implements ConfigFileFormat {
        private final String name;
        private final List<String> extensions;

        private TestFileFormat(String name, String extension) {
            this.name = name;
            this.extensions = Collections.singletonList(extension);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> extensions() {
            return extensions;
        }

        @Override
        public boolean supportsComments() {
            return false;
        }

        @Override
        public ConfigFile open(File file) {
            throw new UnsupportedOperationException();
        }
    }
}
