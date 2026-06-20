package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigInjectionException;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.config.YamlConfigTestUtil;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigModuleListenerTest {

    @TempDir
    File tempDir;

    private final BlueLogBackend previousBackend = BlueLoggerFactory.getBackend();

    @AfterEach
    void resetLogger() {
        BlueLoggerFactory.setBackend(previousBackend);
        BlueLoggerFactory.setDebugEnabled(false);
        Configs.typeHandlers().clear();
    }

    @Test
    void injectsDefaultsAndEnablesDebug() {
        BlueLoggerFactory.setBackend(new TestBackend());
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  debug:\n    enable: true\n");
        ModuleConfigRegistry configRegistry = registry();

        new ConfigModuleListener(configRegistry).onLoadPre(new ModuleLoadEvent.Pre(context));

        ExampleConfig config = configRegistry.getContext(context).get(ExampleConfig.class);
        assertEquals("hello", config.message);
        assertEquals(3, config.amount);
        assertEquals(true, config.enabled);
        assertEquals(TestMode.ACTIVE, config.mode);
        assertEquals(2, config.values.size());
        assertTrue(BlueLoggerFactory.isDebugEnabled());
        assertFalse(context.isEnableSkipped());
    }

    @Test
    void generalEnableFalseBlocksModule() {
        ModuleContext context = new ModuleContext(new ConfiguredModule(), ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  enable: false\n");

        ConfigModuleListener listener = new ConfigModuleListener(tempDir);
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        assertFalse(context.isEnableSkipped());

        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(event);

        assertTrue(event.isCancelled());
        assertEquals("config", event.getCancelOutcome().getSource());
        assertEquals("Module disabled by configuration", event.getCancelOutcome().getMessage());
    }

    @Test
    void enableSkippedClearsDisabledModuleState() {
        ModuleContext context = new ModuleContext(new ConfiguredModule(), ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  enable: false\n");
        ConfigModuleListener listener = new ConfigModuleListener(tempDir);

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));
        ModuleEnableEvent.Pre first = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(first);
        listener.onEnableSkipped(new ModuleEnableEvent.Skipped(context, first.getCancelOutcome()));

        ModuleEnableEvent.Pre second = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(second);

        assertTrue(first.isCancelled());
        assertFalse(second.isCancelled());
    }

    @Test
    void configLoadFailureReportsLoadPreError() {
        ModuleContext context = new ModuleContext(new ConfiguredModule(), ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ConfigInjectionException failure = new ConfigInjectionException(
                "expected load failure",
                new IllegalStateException("broken")
        );
        ConfigRegisterProcessor processor = new ThrowingConfigRegisterProcessor(
                failure,
                null
        );
        ConfigModuleListener listener = new ConfigModuleListener(registry(), processor);
        ModuleLoadEvent.Pre event = new ModuleLoadEvent.Pre(context);

        assertDoesNotThrow(() -> listener.onLoadPre(event));

        assertTrue(event.hasError());
        assertEquals("config", event.getErrorSource());
        assertEquals("Module configuration failed", event.getErrorMessage());
        assertSame(failure, event.getErrorCause());
    }

    @Test
    void disableSaveFailureDoesNotThrowOutOfListener() {
        ModuleContext context = new ModuleContext(new ConfiguredModule(), ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ConfigRegisterProcessor processor = new ThrowingConfigRegisterProcessor(
                null,
                new ConfigInjectionException("expected save failure", new IllegalStateException("broken"))
        );
        ConfigModuleListener listener = new ConfigModuleListener(registry(), processor);

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        assertDoesNotThrow(() -> listener.onDisablePost(new ModuleDisableEvent.Post(context)));
        assertDoesNotThrow(() -> listener.onDisableFailed(new ModuleDisableEvent.Failed(context, new IllegalStateException("disable failed"))));
    }

    @Test
    void defaultConfigFileFormatCreatesYamlConfig() {
        ModuleContext context = new ModuleContext(new DefaultFormatModule(), DefaultFormatModule.class.getAnnotation(ModuleInfo.class));

        new ConfigModuleListener(tempDir).onLoadPre(new ModuleLoadEvent.Pre(context));

        assertTrue(new File(tempDir, "modules/default-format/config.yml").exists());
        assertFalse(new File(tempDir, "modules/default-format/config.json").exists());
    }

    @Test
    void moduleConfigAlwaysUsesYmlEvenWhenJsonExists() {
        ModuleContext context = new ModuleContext(new JsonFilePresentModule(), JsonFilePresentModule.class.getAnnotation(ModuleInfo.class));
        File jsonFile = new File(tempDir, "modules/json-file-present/config.json");
        YamlConfigTestUtil.write(jsonFile, "{\"general\":{\"enable\":false}}\n");

        new ConfigModuleListener(tempDir).onLoadPre(new ModuleLoadEvent.Pre(context));

        File ymlFile = new File(tempDir, "modules/json-file-present/config.yml");
        assertTrue(ymlFile.exists());
        assertTrue(jsonFile.exists());
        assertFalse(context.isEnableSkipped());
    }

    @Test
    void existingYmlConfigIsUsed() {
        ModuleContext context = new ModuleContext(new ExistingYmlModule(), ExistingYmlModule.class.getAnnotation(ModuleInfo.class));
        File yamlFile = new File(tempDir, "modules/existing-yml/config.yml");
        YamlConfigTestUtil.write(yamlFile, "general:\n  enable: true\n");

        new ConfigModuleListener(tempDir).onLoadPre(new ModuleLoadEvent.Pre(context));

        assertTrue(yamlFile.exists());
        assertFalse(new File(tempDir, "modules/existing-yml/config.json").exists());
    }

    @Test
    void contextReturnsRegisteredInstance() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = registry();

        new ConfigModuleListener(configRegistry).onLoadPre(new ModuleLoadEvent.Pre(context));

        ModuleConfigContext moduleConfigContext = configRegistry.getContext(context);
        ExampleConfig config = moduleConfigContext.get(ExampleConfig.class);
        assertSame(config, moduleConfigContext.get(ExampleConfig.class));
    }

    @Test
    void disablePostSavesRegisteredConfigObjectValues() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = registry();
        ConfigModuleListener listener = new ConfigModuleListener(configRegistry);

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        ExampleConfig config = configRegistry.getContext(context).get(ExampleConfig.class);
        config.amount = 5;
        config.values = Arrays.asList(3, 4);

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        File file = new File(tempDir, "modules/configured/config.yml");
        assertEquals(5, Configs.yaml(file).getInt("example.amount"));
        assertEquals(Arrays.asList(3, 4), Configs.yaml(file).getList("example.values", Integer.class));
    }

    @Test
    void blueConfigCanWriteToNamedFile() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = registry();

        new ConfigModuleListener(configRegistry).onLoadPre(new ModuleLoadEvent.Pre(context));

        File defaultFile = new File(tempDir, "modules/configured/config.yml");
        File databaseFile = new File(tempDir, "modules/configured/database.yml");
        assertTrue(defaultFile.exists());
        assertTrue(databaseFile.exists());
        assertEquals("hello", Configs.yaml(defaultFile).getString("example.message"));
        assertEquals("localhost", Configs.yaml(databaseFile).getString("database.host"));
        assertEquals("localhost", configRegistry.getContext(context).get(DatabaseConfig.class).host);
    }

    @Test
    void blueConfigFileNameWithYmlSuffixIsNotDuplicated() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));

        new ConfigModuleListener(registry()).onLoadPre(new ModuleLoadEvent.Pre(context));

        assertTrue(new File(tempDir, "modules/configured/metrics.yml").exists());
        assertFalse(new File(tempDir, "modules/configured/metrics.yml.yml").exists());
    }

    @Test
    void disablePostSavesNamedConfigFileValues() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = registry();
        ConfigModuleListener listener = new ConfigModuleListener(configRegistry);

        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        DatabaseConfig config = configRegistry.getContext(context).get(DatabaseConfig.class);
        config.host = "127.0.0.1";

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        File databaseFile = new File(tempDir, "modules/configured/database.yml");
        assertEquals("127.0.0.1", Configs.yaml(databaseFile).getString("database.host"));
    }

    @Test
    void contextRejectsUnregisteredType() {
        ConfiguredModule module = new ConfiguredModule();
        ModuleContext context = new ModuleContext(module, ConfiguredModule.class.getAnnotation(ModuleInfo.class));
        ModuleConfigRegistry configRegistry = registry();

        new ConfigModuleListener(configRegistry).onLoadPre(new ModuleLoadEvent.Pre(context));

        ModuleConfigContext moduleConfigContext = configRegistry.getContext(context);
        assertThrows(IllegalStateException.class, () -> moduleConfigContext.get(UnregisteredConfig.class));
    }

    @Test
    void resolverInjectsModuleConfigContextConstructorParameter() {
        ModuleConfigRegistry configRegistry = registry();
        ConfigModuleListener listener = new ConfigModuleListener(configRegistry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = new ModuleCandidate(
                ConstructorContextModule.class,
                ConstructorContextModule.class.getAnnotation(ModuleInfo.class)
        );

        parameterResolvers.registerIfAbsent(new ConfigContextResolver(configRegistry));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertEquals(1, parameterResolvers.resolvers().size());

        ConstructorContextModule module = (ConstructorContextModule) new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate);
        assertThrows(IllegalStateException.class, () -> module.configContext.get(ExampleConfig.class));

        ModuleContext context = new ModuleContext(module, candidate);
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        ExampleConfig config = module.configContext.get(ExampleConfig.class);
        assertEquals("hello", config.message);
        assertSame(module.configContext, configRegistry.getContext(context));
    }

    @Test
    void resolverInjectsModuleConfigContextField() {
        ModuleConfigRegistry configRegistry = registry();
        ConfigModuleListener listener = new ConfigModuleListener(configRegistry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = new ModuleCandidate(
                FieldContextModule.class,
                FieldContextModule.class.getAnnotation(ModuleInfo.class)
        );

        parameterResolvers.registerIfAbsent(new ConfigContextResolver(configRegistry));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        FieldContextModule module = (FieldContextModule) new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate);
        ModuleContext context = new ModuleContext(module, candidate);
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        assertEquals("hello", module.configContext.get(ExampleConfig.class).message);
        assertSame(module.configContext, configRegistry.getContext(context));
    }

    @ModuleInfo(id = "configured", name = "Configured")
    private static class ConfiguredModule implements Module {
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

    @ModuleInfo(id = "default-format", name = "Default Format")
    private static class DefaultFormatModule implements Module {
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

    @ModuleInfo(id = "json-file-present", name = "Json File Present")
    private static class JsonFilePresentModule implements Module {
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

    @ModuleInfo(id = "existing-yml", name = "Existing YML")
    private static class ExistingYmlModule implements Module {
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

    @ModuleInfo(id = "constructor-context", name = "Constructor Context")
    private static class ConstructorContextModule implements Module {
        private final ModuleConfigContext configContext;

        private ConstructorContextModule(ModuleConfigContext configContext) {
            this.configContext = configContext;
        }

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

    @ModuleInfo(id = "field-context", name = "Field Context")
    private static class FieldContextModule implements Module {
        @ModuleInject
        private ModuleConfigContext configContext;

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
    @BlueConfig(category = "example")
    private static class ExampleConfig {
        @BlueConfig.Field(path = "message", defaultValue = "hello", comment = "message comment")
        private String message;

        @BlueConfig.Field(path = "amount", defaultValue = "3")
        private int amount;

        @BlueConfig.Field(path = "enabled", defaultValue = "true")
        private boolean enabled;

        @BlueConfig.Field(path = "mode", defaultValue = "ACTIVE")
        private TestMode mode;

        @BlueConfig.Field(path = "values", defaultValue = {"1", "2"})
        private List<Integer> values;
    }

    @ConfigRegister
    @BlueConfig(category = "database", file = "database")
    private static class DatabaseConfig {
        @BlueConfig.Field(path = "host", defaultValue = "localhost")
        private String host;
    }

    @ConfigRegister
    @BlueConfig(category = "metrics", file = "metrics.yml")
    private static class MetricsConfig {
        @BlueConfig.Field(path = "enabled", defaultValue = "true")
        private boolean enabled;
    }

    private enum TestMode {
        ACTIVE
    }

    private static class UnregisteredConfig {
    }

    private static class ThrowingConfigRegisterProcessor extends ConfigRegisterProcessor {
        private final RuntimeException processFailure;
        private final RuntimeException saveFailure;

        private ThrowingConfigRegisterProcessor(RuntimeException processFailure, RuntimeException saveFailure) {
            this.processFailure = processFailure;
            this.saveFailure = saveFailure;
        }

        @Override
        public void process(ModuleContext moduleContext, ModuleConfigState configState) {
            if (processFailure != null) {
                throw processFailure;
            }
        }

        @Override
        public void save(ModuleConfigState configState) {
            if (saveFailure != null) {
                throw saveFailure;
            }
        }
    }

    private ModuleConfigRegistry registry() {
        return new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
    }

    private static class TestBackend implements BlueLogBackend {
        @Override
        public boolean isEnabled(BlueLogLevel level) {
            return true;
        }

        @Override
        public void log(BlueLogLevel level, String message) {
        }

        @Override
        public void log(BlueLogLevel level, String message, Throwable throwable) {
        }
    }
}
