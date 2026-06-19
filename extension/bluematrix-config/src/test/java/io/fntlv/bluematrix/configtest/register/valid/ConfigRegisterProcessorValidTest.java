package io.fntlv.bluematrix.configtest.register.valid;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFile;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigInjectionException;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.config.extension.ModuleConfigRegistry;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRegisterProcessorValidTest {

    @TempDir
    File tempDir;

    @Test
    void registersAndLoadsConfigClass() {
        TestModule module = new TestModule();
        ModuleContext moduleContext = new ModuleContext(module, TestModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ModuleConfigState configState = new ModuleConfigState(
                module,
                moduleContext.getInfo().id(),
                registry.openFile(moduleContext.getInfo().id())
        );

        new ConfigRegisterProcessor().process(moduleContext, configState);

        ProcessorConfig config = configState.get(ProcessorConfig.class);
        assertEquals("processor", config.name);
        assertEquals(5, config.amount);
        assertEquals(Arrays.asList("mysql", "redis"), config.libraries);
        assertEquals(Arrays.asList("mysql", "redis"), configState.file().getList("processor.libraries", String.class));
        assertTrue(configState.file().get("processor.libraries") instanceof List);
        assertEquals(Arrays.asList("name", "amount", "libraries"),
                new ArrayList<>(((YamlConfigFile) configState.file()).getKeys("processor")));
    }

    @Test
    void duplicateProcessFailsOnDuplicateConfigRegistration() {
        TestModule module = new TestModule();
        ModuleContext moduleContext = new ModuleContext(module, TestModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ModuleConfigState configState = new ModuleConfigState(
                module,
                moduleContext.getInfo().id(),
                registry.openFile(moduleContext.getInfo().id())
        );
        ConfigRegisterProcessor processor = new ConfigRegisterProcessor();

        processor.process(moduleContext, configState);

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> processor.process(moduleContext, configState));
        assertExceptionMessageContains(exception, "Config type is already registered for module");
    }

    @ModuleInfo(id = "processor-valid", name = "Processor Valid")
    private static class TestModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ConfigRegister
    @BlueConfig(category = "processor")
    private static class ProcessorConfig {
        @BlueConfig.Field(path = "name", defaultValue = "processor")
        private String name;

        @BlueConfig.Field(path = "amount", defaultValue = "5")
        private int amount;

        @BlueConfig.Field(path = "libraries", defaultValue = {"mysql", "redis"})
        private List<String> libraries;
    }

    private static void assertExceptionMessageContains(Throwable throwable, String expected) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return;
            }
            current = current.getCause();
        }
        throw new AssertionError("Expected exception message to contain: " + expected);
    }
}
