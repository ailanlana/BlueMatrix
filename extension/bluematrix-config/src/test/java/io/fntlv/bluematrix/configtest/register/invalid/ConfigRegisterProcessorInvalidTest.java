package io.fntlv.bluematrix.configtest.register.invalid;

import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigRegisterProcessorInvalidTest {

    @TempDir
    File tempDir;

    @Test
    void rejectsRegisteredClassWithoutBlueConfig() {
        TestModule module = new TestModule();
        ModuleContext moduleContext = new ModuleContext(module, TestModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ModuleConfigState configState = new ModuleConfigState(
                module,
                moduleContext.getInfo().id(),
                registry.openFile(moduleContext.getInfo().id())
        );

        assertThrows(ConfigInjectionException.class,
                () -> new ConfigRegisterProcessor().process(moduleContext, configState));
    }

    @ModuleInfo(id = "processor-invalid", name = "Processor Invalid")
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
    private static class MissingBlueConfig {
    }
}
