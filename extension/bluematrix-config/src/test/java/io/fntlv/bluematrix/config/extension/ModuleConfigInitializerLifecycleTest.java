package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.YamlConfigTestUtil;
import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigInjectionException;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigInitializerLifecycleTest {
    @TempDir
    File tempDir;

    @Test
    void loadRegistersConfigAndWritesDefaults() {
        ModuleConfigInitializer initializer = initializer();
        ModuleContext context = context();
        ModuleConfigState state = ConfigCapabilityTestSupport.state(tempDir, context);

        initializer.initialize(context, state);

        LifecycleConfig config = state.get(LifecycleConfig.class);
        assertTrue(state.moduleEnabled());
        assertEquals("hello", config.message);
        assertEquals(3, config.amount);
        assertSame(config, state.get(LifecycleConfig.class));
        File file = new File(tempDir, "modules/lifecycle/config.yml");
        assertEquals("hello", Configs.yaml(file).getString("lifecycle.message"));
        assertEquals(3, Configs.yaml(file).getInt("lifecycle.amount"));
    }

    @Test
    void loadReadsExistingConfigValues() {
        ModuleConfigInitializer initializer = initializer();
        ModuleContext context = context();
        File file = new File(tempDir, "modules/lifecycle/config.yml");
        YamlConfigTestUtil.write(file, "lifecycle:\n  message: existing\n  amount: 7\n");
        ModuleConfigState state = ConfigCapabilityTestSupport.state(tempDir, context);

        initializer.initialize(context, state);

        LifecycleConfig config = state.get(LifecycleConfig.class);
        assertEquals("existing", config.message);
        assertEquals(7, config.amount);
    }

    @Test
    void saveWritesCurrentConfigValues() {
        ModuleConfigInitializer initializer = initializer();
        ModuleContext context = context();
        ModuleConfigState state = ConfigCapabilityTestSupport.state(tempDir, context);

        initializer.initialize(context, state);
        LifecycleConfig config = state.get(LifecycleConfig.class);
        config.message = "saved";
        config.amount = 9;
        initializer.save(context, state);

        File file = new File(tempDir, "modules/lifecycle/config.yml");
        assertEquals("saved", Configs.yaml(file).getString("lifecycle.message"));
        assertEquals(9, Configs.yaml(file).getInt("lifecycle.amount"));
    }

    @Test
    void loadReturnsDisabledWhenGeneralEnableIsFalse() {
        ModuleConfigInitializer initializer = initializer();
        ModuleContext context = context();
        File file = new File(tempDir, "modules/lifecycle/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  enable: false\n");
        ModuleConfigState state = ConfigCapabilityTestSupport.state(tempDir, context);

        initializer.initialize(context, state);

        assertFalse(state.moduleEnabled());
    }

    @Test
    void loadFailurePropagatesFromLifecycle() {
        ConfigInjectionException failure = new ConfigInjectionException(
                "expected load failure",
                new IllegalStateException("broken")
        );
        ModuleConfigInitializer initializer = ConfigCapabilityTestSupport.initializer(
                tempDir,
                new ThrowingConfigRegisterProcessor(failure)
        );
        ModuleContext context = context();
        ModuleConfigState state = ConfigCapabilityTestSupport.state(tempDir, context);

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> initializer.initialize(context, state));

        assertSame(failure, exception);
    }

    private ModuleConfigInitializer initializer() {
        return ConfigCapabilityTestSupport.initializer(tempDir, new ConfigRegisterProcessor());
    }

    private ModuleContext context() {
        LifecycleModule module = new LifecycleModule();
        return new ModuleContext(module, LifecycleModule.class.getAnnotation(ModuleInfo.class));
    }

    @ModuleInfo(id = "lifecycle", name = "Lifecycle")
    private static class LifecycleModule implements Module {
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
    @BlueConfig(category = "lifecycle")
    private static class LifecycleConfig {
        @BlueConfig.Field(path = "message", defaultValue = "hello")
        private String message;

        @BlueConfig.Field(path = "amount", defaultValue = "3")
        private int amount;
    }

    private static class ThrowingConfigRegisterProcessor extends ConfigRegisterProcessor {
        private final RuntimeException failure;

        private ThrowingConfigRegisterProcessor(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void process(ModuleContext moduleContext, ModuleConfigState configState) {
            throw failure;
        }
    }
}
