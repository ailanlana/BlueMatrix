package io.fntlv.bluematrix.configtest.customtype;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.type.complex.ComplexTypeHandlers;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.extension.ConfigModuleListener;
import io.fntlv.bluematrix.config.extension.ModuleConfigRegistry;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigModuleListenerCustomTypeTest {

    @TempDir
    File tempDir;

    @AfterEach
    void resetTypeHandlers() {
        Configs.typeHandlers().clear();
    }

    @Test
    void customTypeHandlerWritesDefaultAndRegistersValue() {
        Configs.typeHandlers().register(ComplexTypeHandlers.forType(CustomPoint.class)
                .onStringDeserialize((value, type) -> {
                    String[] parts = value.split(",");
                    return new CustomPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                })
                .onConfigSave((section, value) -> {
                    section.set("x", value.x);
                    section.set("y", value.y);
                })
                .onConfigLoad((section, type) -> new CustomPoint(section.getInt("x"), section.getInt("y"))));
        CustomTypeModule module = new CustomTypeModule();
        ModuleContext context = new ModuleContext(module, CustomTypeModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());

        new ConfigModuleListener(configRegistry).onLoadPre(new ModuleLoadEvent.Pre(context));

        CustomTypeConfig config = configRegistry.getContext(context).get(CustomTypeConfig.class);
        assertEquals(7, config.point.x);
        assertEquals(9, config.point.y);

        ConfigFile file = Configs.yaml(new File(tempDir, "modules/custom-type/config.yml"));
        assertEquals(7, file.getInt("custom.point.x"));
        assertEquals(9, file.getInt("custom.point.y"));
    }

    @ModuleInfo(id = "custom-type", name = "Custom Type")
    private static class CustomTypeModule implements Module {
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
    @BlueConfig(category = "custom")
    private static class CustomTypeConfig {
        @BlueConfig.Field(path = "point", defaultValue = "7,9")
        private CustomPoint point;
    }

    private static class CustomPoint {
        private final int x;
        private final int y;

        private CustomPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
