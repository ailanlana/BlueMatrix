package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.YamlConfigTestUtil;
import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigInjectionException;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContextResolver;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityListener;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityRegistry;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.OtherInjectionContext;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogLevel;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.logging.backend.BlueLogBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCapabilityTest {
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
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  debug:\n    enable: true\n");

        ModuleConfigContext configContext = load(context);

        ExampleConfig config = configContext.get(ExampleConfig.class);
        assertEquals("hello", config.message);
        assertEquals(3, config.amount);
        assertTrue(config.enabled);
        assertEquals(TestMode.ACTIVE, config.mode);
        assertEquals(2, config.values.size());
        assertTrue(BlueLoggerFactory.isDebugEnabled());
    }

    @Test
    void generalEnableFalseBlocksModule() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  enable: false\n");
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);

        registerAndLoad(listener, context);
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(event);

        assertTrue(event.isCancelled());
        assertEquals("config", event.getCancelOutcome().getSource());
        assertEquals("Module disabled by configuration", event.getCancelOutcome().getMessage());
    }

    @Test
    void enableSkippedKeepsLoadedDisabledModuleState() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        File file = new File(tempDir, "modules/configured/config.yml");
        YamlConfigTestUtil.write(file, "general:\n  enable: false\n");
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);

        registerAndLoad(listener, context);
        ModuleEnableEvent.Pre first = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(first);
        listener.onEnableSkipped(new ModuleEnableEvent.Skipped(context, first.getCancelOutcome()));
        ModuleEnableEvent.Pre second = new ModuleEnableEvent.Pre(context);
        listener.onEnablePre(second);

        assertTrue(first.isCancelled());
        assertTrue(second.isCancelled());
    }

    @Test
    void configLoadFailureReportsLoadPreError() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        ConfigInjectionException failure = new ConfigInjectionException(
                "expected load failure",
                new IllegalStateException("broken")
        );
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = ConfigCapabilityTestSupport.capability(
                tempDir,
                new ThrowingConfigRegisterProcessor(failure, null)
        );
        ModuleCapabilityListener listener = listener(capability);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(context)));
        ModuleLoadEvent.Pre event = new ModuleLoadEvent.Pre(context);

        assertDoesNotThrow(() -> listener.onLoadPre(event));

        assertTrue(event.hasError());
        assertEquals("config", event.getErrorSource());
        assertEquals("Module configuration failed", event.getErrorMessage());
        assertSame(failure, event.getErrorCause());
    }

    @Test
    void defaultConfigFileFormatCreatesYamlConfig() {
        ModuleContext context = context(new DefaultFormatModule(), DefaultFormatModule.class);

        load(context);

        assertTrue(new File(tempDir, "modules/default-format/config.yml").exists());
        assertFalse(new File(tempDir, "modules/default-format/config.json").exists());
    }

    @Test
    void moduleConfigAlwaysUsesYmlEvenWhenJsonExists() {
        ModuleContext context = context(new JsonFilePresentModule(), JsonFilePresentModule.class);
        File jsonFile = new File(tempDir, "modules/json-file-present/config.json");
        YamlConfigTestUtil.write(jsonFile, "{\"general\":{\"enable\":false}}\n");

        load(context);

        assertTrue(new File(tempDir, "modules/json-file-present/config.yml").exists());
        assertTrue(jsonFile.exists());
    }

    @Test
    void contextReturnsRegisteredInstance() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);

        ModuleConfigContext configContext = load(context);

        ExampleConfig config = configContext.get(ExampleConfig.class);
        assertSame(config, configContext.get(ExampleConfig.class));
    }

    @Test
    void disablePostSavesRegisteredConfigObjectValues() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);
        registerAndLoad(listener, context);
        ExampleConfig config = capability.context(context.id()).get(ExampleConfig.class);
        config.amount = 5;
        config.values = Arrays.asList(3, 4);

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        File file = new File(tempDir, "modules/configured/config.yml");
        assertEquals(5, Configs.yaml(file).getInt("example.amount"));
        assertEquals(Arrays.asList(3, 4), Configs.yaml(file).getList("example.values", Integer.class));
    }

    @Test
    void blueConfigCanWriteToNamedFile() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);

        ModuleConfigContext configContext = load(context);

        File defaultFile = new File(tempDir, "modules/configured/config.yml");
        File databaseFile = new File(tempDir, "modules/configured/database.yml");
        assertTrue(defaultFile.exists());
        assertTrue(databaseFile.exists());
        assertEquals("hello", Configs.yaml(defaultFile).getString("example.message"));
        assertEquals("localhost", Configs.yaml(databaseFile).getString("database.host"));
        assertEquals("localhost", configContext.get(DatabaseConfig.class).host);
    }

    @Test
    void blueConfigFileNameWithYmlSuffixIsNotDuplicated() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);

        load(context);

        assertTrue(new File(tempDir, "modules/configured/metrics.yml").exists());
        assertFalse(new File(tempDir, "modules/configured/metrics.yml.yml").exists());
    }

    @Test
    void disablePostSavesNamedConfigFileValues() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityListener listener = listener(capability);
        registerAndLoad(listener, context);
        DatabaseConfig config = capability.context(context.id()).get(DatabaseConfig.class);
        config.host = "127.0.0.1";

        listener.onDisablePost(new ModuleDisableEvent.Post(context));

        File databaseFile = new File(tempDir, "modules/configured/database.yml");
        assertEquals("127.0.0.1", Configs.yaml(databaseFile).getString("database.host"));
    }

    @Test
    void contextRejectsUnregisteredType() {
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);
        ModuleConfigContext configContext = load(context);

        assertThrows(IllegalStateException.class, () -> configContext.get(UnregisteredConfig.class));
    }

    @Test
    void resolverInjectsModuleConfigContextConstructorParameter() {
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityRegistry registry = ConfigCapabilityTestSupport.registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = new ModuleCandidate(
                ConstructorContextModule.class,
                ConstructorContextModule.class.getAnnotation(ModuleInfo.class)
        );

        parameterResolvers.registerIfAbsent(new ModuleCapabilityContextResolver(registry));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        ConstructorContextModule module = (ConstructorContextModule) new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate);
        assertThrows(IllegalStateException.class, () -> module.configContext.get(ExampleConfig.class));

        ModuleContext context = new ModuleContext(module, candidate);
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));

        ExampleConfig config = module.configContext.get(ExampleConfig.class);
        assertEquals("hello", config.message);
        assertSame(module.configContext, capability.context(context.id()));
    }

    @Test
    void resolverInjectsModuleConfigContextIntoOtherConstructorParameter() {
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability();
        ModuleCapabilityRegistry registry = ConfigCapabilityTestSupport.registry(capability);
        ModuleCapabilityListener listener = new ModuleCapabilityListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleContext context = context(new ConfiguredModule(), ConfiguredModule.class);

        parameterResolvers.registerIfAbsent(new ModuleCapabilityContextResolver(registry));
        registerAndLoad(listener, context);

        OtherConstructorComponent component = new DefaultModuleInstanceFactory(parameterResolvers)
                .createOther(OtherConstructorComponent.class, OtherInjectionContext.from(context));

        assertSame(capability.context(context.id()), component.configContext);
        assertEquals("hello", component.configContext.get(ExampleConfig.class).message);
    }

    private ModuleConfigContext load(ModuleContext context) {
        return ConfigCapabilityTestSupport.load(tempDir, context);
    }

    private ModuleCapability<ModuleConfigContext, ModuleConfigState> capability() {
        return ConfigCapabilityTestSupport.capability(tempDir);
    }

    private ModuleCapabilityListener listener(ModuleCapability<ModuleConfigContext, ModuleConfigState> capability) {
        return ConfigCapabilityTestSupport.listener(capability);
    }

    private void registerAndLoad(ModuleCapabilityListener listener, ModuleContext context) {
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(context)));
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));
    }

    private static ModuleCandidate candidate(ModuleContext context) {
        return new ModuleCandidate(context.getModuleClass(), context.getDescriptor());
    }

    private static ModuleContext context(Module module, Class<? extends Module> type) {
        return new ModuleContext(module, type.getAnnotation(ModuleInfo.class));
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

    private static class OtherConstructorComponent {
        private final ModuleConfigContext configContext;

        private OtherConstructorComponent(ModuleConfigContext configContext) {
            this.configContext = configContext;
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
