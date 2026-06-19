package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RegisteredConfigBinderTest {

    @TempDir
    File tempDir;

    @Test
    void createsRegisteredConfigAndLoadsDefaultValues() {
        RegisteredConfigBinder binder = new RegisteredConfigBinder();
        BinderConfig instance = new BinderConfig();
        ConfigFile file = Configs.yaml(new File(tempDir, "binder.yml"));

        RegisteredConfig config = binder.create(instance, BinderConfig.class, file, "binder");
        binder.load(config);

        assertSame(instance, config.instance());
        assertEquals(BinderConfig.class, config.type());
        assertEquals(file, config.file());
        assertEquals("hello", instance.message);
        assertEquals(TestMode.ACTIVE, instance.mode);
        assertEquals(Arrays.asList(1, 2), instance.values);
    }

    @Test
    void savesFieldValuesToConfigFile() {
        RegisteredConfigBinder binder = new RegisteredConfigBinder();
        BinderConfig instance = new BinderConfig();
        ConfigFile file = Configs.yaml(new File(tempDir, "binder.yml"));
        RegisteredConfig config = binder.create(instance, BinderConfig.class, file, "binder");
        binder.load(config);

        instance.message = "updated";
        instance.mode = TestMode.PASSIVE;
        instance.values = Arrays.asList(3, 4);

        binder.save(config);
        file.saveIfChanged();

        ConfigFile saved = Configs.yaml(new File(tempDir, "binder.yml"));
        assertEquals("updated", saved.getString("binder.message"));
        assertEquals("PASSIVE", saved.getString("binder.mode"));
        assertEquals(Arrays.asList(3, 4), saved.getList("binder.values", Integer.class));
    }

    private enum TestMode {
        ACTIVE,
        PASSIVE
    }

    private static class BinderConfig {
        @BlueConfig.Field(path = "message", defaultValue = "hello")
        private String message;

        @BlueConfig.Field(path = "mode", defaultValue = "ACTIVE")
        private TestMode mode;

        @BlueConfig.Field(path = "values", defaultValue = {"1", "2"})
        private List<Integer> values;
    }
}
