package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.extension.register.ConfigDefinitionException;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.register.ConfigInjectionException;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.ModuleConfigRegistry;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRegisterProcessorTest {

    @TempDir
    File tempDir;

    @Test
    void invalidConfigDefinitionIsWrappedAsConfigInjectionException() throws Exception {
        InvalidDefinitionModule module = new InvalidDefinitionModule();
        ModuleContext context = new ModuleContext(
                module,
                InvalidDefinitionModule.class.getAnnotation(ModuleInfo.class)
        );
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ConfigRegisterProcessor processor = new ConfigRegisterProcessor();
        ModuleConfigState state = new ModuleConfigState(
                module,
                context.id(),
                registry.openFile(context.id())
        );

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> invokeRegisterConfigClass(processor, context, state, MissingBlueConfig.class));

        assertTrue(containsCause(exception, ConfigDefinitionException.class));
    }

    @Test
    void configConstructorFailureKeepsOriginalCause() {
        InvalidDefinitionModule module = new InvalidDefinitionModule();
        ModuleContext context = new ModuleContext(
                module,
                InvalidDefinitionModule.class.getAnnotation(ModuleInfo.class)
        );
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ConfigRegisterProcessor processor = new ConfigRegisterProcessor();
        ModuleConfigState state = new ModuleConfigState(
                module,
                context.id(),
                registry.openFile(context.id())
        );

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> invokeRegisterConfigClass(processor, context, state, ThrowingConstructorConfig.class));

        assertTrue(containsCause(exception, ConfigDefinitionException.class));
        assertTrue(containsCause(exception, IllegalStateException.class));
        assertTrue(!containsCause(exception, InvocationTargetException.class));
    }

    @Test
    void invalidBlueConfigFileNameIsWrappedAsConfigInjectionException() {
        InvalidDefinitionModule module = new InvalidDefinitionModule();
        ModuleContext context = new ModuleContext(
                module,
                InvalidDefinitionModule.class.getAnnotation(ModuleInfo.class)
        );
        ModuleConfigRegistry registry = new ModuleConfigRegistry(tempDir, new YamlConfigFileFormat());
        ConfigRegisterProcessor processor = new ConfigRegisterProcessor();
        ModuleConfigState state = new ModuleConfigState(
                module,
                context.id(),
                fileName -> registry.openFile(context.id(), fileName)
        );

        ConfigInjectionException exception = assertThrows(ConfigInjectionException.class,
                () -> invokeRegisterConfigClass(processor, context, state, UnsafeFileNameConfig.class));

        assertTrue(containsCause(exception, IllegalArgumentException.class));
    }


    private void invokeRegisterConfigClass(ConfigRegisterProcessor processor,
                                           ModuleContext context,
                                           ModuleConfigState state,
                                           Class<?> configClass) {
        try {
            Method method = ConfigRegisterProcessor.class.getDeclaredMethod(
                    "registerConfigClass",
                    ModuleContext.class,
                    ModuleConfigState.class,
                    Class.class
            );
            method.setAccessible(true);
            method.invoke(processor, context, state, configClass);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @ModuleInfo(id = "invalid-definition", name = "Invalid Definition")
    private static class InvalidDefinitionModule implements Module {
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

    private static class MissingBlueConfig {
    }

    @BlueConfig
    private static class ThrowingConstructorConfig {
        private ThrowingConstructorConfig() {
            throw new IllegalStateException("config constructor failed");
        }
    }

    @BlueConfig(file = "../database")
    private static class UnsafeFileNameConfig {
        @BlueConfig.Field(path = "host", defaultValue = "localhost")
        private String host;
    }
}
