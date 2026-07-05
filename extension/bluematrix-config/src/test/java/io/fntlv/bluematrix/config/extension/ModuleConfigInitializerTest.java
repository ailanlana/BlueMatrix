package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleConfigInitializerTest {

    @TempDir
    File tempDir;

    @Test
    void modulePathUsesModulesDirectory() {
        ModuleConfigInitializer initializer = ConfigCapabilityTestSupport.initializer(tempDir);

        assertEquals(new File(tempDir, "modules/example"), initializer.modulePath("example"));
    }

    @Test
    void initializerUsesExplicitYamlFormat() {
        ModuleConfigInitializer initializer = new ModuleConfigInitializer(
                tempDir,
                new YamlConfigFileFormat(),
                new io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor()
        );

        assertEquals("yaml", initializer.fileFormat().name());
    }

    @Test
    void opensNamedYamlConfigFileInModulesDirectory() {
        ModuleConfigInitializer initializer = ConfigCapabilityTestSupport.initializer(tempDir);

        assertEquals(new File(tempDir, "modules/example/database.yml"),
                initializer.openFile("example", "database").getFile());
        assertEquals(new File(tempDir, "modules/example/database.yml"),
                initializer.openFile("example", "database.yml").getFile());
    }

    @Test
    void rejectsUnsafeConfigFileNames() {
        ModuleConfigInitializer initializer = ConfigCapabilityTestSupport.initializer(tempDir);

        assertThrows(IllegalArgumentException.class, () -> initializer.openFile("example", " "));
        assertThrows(IllegalArgumentException.class, () -> initializer.openFile("example", "../database"));
        assertThrows(IllegalArgumentException.class, () -> initializer.openFile("example", "folder/database"));
        assertThrows(IllegalArgumentException.class, () -> initializer.openFile("example", "folder\\database"));
    }

    @Test
    void contextUsesStateForModuleAndConfigs() {
        ExampleModule module = new ExampleModule();
        ModuleConfigState state = new ModuleConfigState(
                module,
                "example",
                ConfigCapabilityTestSupport.openFile(tempDir, "example")
        );
        ModuleConfigContext context = new ModuleConfigContext("example", state);

        assertEquals("example", context.moduleId());
        assertSame(module, context.module());
        assertThrows(IllegalStateException.class, () -> context.get(ExampleModule.class));
    }

    @Test
    void singleFileStateUsesFileNameNormalization() {
        ExampleModule module = new ExampleModule();
        ModuleConfigState state = new ModuleConfigState(
                module,
                "example",
                ConfigCapabilityTestSupport.openFile(tempDir, "example")
        );

        assertSame(state.file(), state.file("config"));
        assertSame(state.file(), state.file("config.yml"));
        assertThrows(IllegalStateException.class, () -> state.file("database"));
        assertThrows(IllegalArgumentException.class, () -> state.file("../config"));
    }

    @Test
    void contextRejectsModuleBeforeStateIsBound() {
        ModuleConfigState state = ConfigCapabilityTestSupport.initializer(tempDir).createState("example");
        ModuleConfigContext context = new ModuleConfigContext("example", state);

        assertThrows(IllegalStateException.class, context::module);
    }

    @Test
    void stateRejectsSecondModuleBinding() {
        ExampleModule module = new ExampleModule();
        ModuleConfigState state = ConfigCapabilityTestSupport.initializer(tempDir).createState("example");

        state.bind(module);

        assertThrows(IllegalStateException.class, () -> state.bind(new ExampleModule()));
    }

    @ModuleInfo(id = "example", name = "Example")
    private static class ExampleModule implements Module {
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
}
