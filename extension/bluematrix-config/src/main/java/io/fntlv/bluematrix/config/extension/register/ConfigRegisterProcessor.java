package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.extension.annotation.ConfigRegister;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleReflectionsFactory;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class ConfigRegisterProcessor {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ConfigRegisterProcessor.class);
    private final RegisteredConfigBinder binder;

    public ConfigRegisterProcessor() {
        this(new RegisteredConfigBinder());
    }

    ConfigRegisterProcessor(RegisteredConfigBinder binder) {
        if (binder == null) {
            throw new IllegalArgumentException("binder cannot be null");
        }
        this.binder = binder;
    }

    public void process(ModuleContext moduleContext, ModuleConfigState configState) {
        process(moduleContext.getInstance().getClass().getSimpleName(),
                moduleContext.id(),
                moduleContext.getReflections().getTypesAnnotatedWith(ConfigRegister.class),
                configState);
    }

    public void process(ModuleCandidate candidate, ModuleConfigState configState) {
        process(candidate.getModuleClass().getSimpleName(),
                candidate.id(),
                ModuleReflectionsFactory.create(candidate.getModuleClass(), candidate.getDescriptor())
                        .getTypesAnnotatedWith(ConfigRegister.class),
                configState);
    }

    private void process(String moduleClassName,
                         String moduleId,
                         Set<Class<?>> configClasses,
                         ModuleConfigState configState) {
        logger().debug("Found {} config register classes in module {}",
                configClasses.size(),
                moduleId);

        for (Class<?> configClass : configClasses) {
            registerConfigClass(moduleClassName, configState, configClass);
        }
    }

    private void registerConfigClass(ModuleContext moduleContext,
                                     ModuleConfigState configState,
                                     Class<?> configClass) {
        registerConfigClass(moduleContext.getInstance().getClass().getSimpleName(), configState, configClass);
    }

    private void registerConfigClass(String moduleClassName,
                                     ModuleConfigState configState,
                                     Class<?> configClass) {
        try {
            BlueConfig classConfig = configClass.getAnnotation(BlueConfig.class);
            if (classConfig == null) {
                throw new ConfigDefinitionException("@ConfigRegister class must be annotated with @BlueConfig");
            }

            Object configInstance = createConfig(configClass);
            ConfigFile file = configState.file(classConfig.file());
            RegisteredConfig registeredConfig = binder.create(
                    configInstance,
                    configClass,
                    file,
                    classConfig.category()
            );
            configState.register(registeredConfig);
            binder.load(registeredConfig);
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration injection failed: %s.%s (%s)",
                    moduleClassName,
                    configClass.getSimpleName(),
                    e.getMessage());
            throw new ConfigInjectionException(errorMsg, e);
        }
    }

    private Object createConfig(Class<?> configClass) {
        try {
            java.lang.reflect.Constructor<?> constructor = configClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ConfigDefinitionException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new ConfigDefinitionException("Failed to create config instance: " + configClass.getName(),
                    cause == null ? e : cause);
        } catch (Exception e) {
            throw new ConfigDefinitionException("Failed to create config instance: " + configClass.getName(), e);
        }
    }

    public void save(ModuleConfigState configState) {
        for (RegisteredConfig config : configState.registeredConfigs()) {
            binder.save(config);
        }
    }

    private BlueLogger logger() {
        return LOGGER;
    }
}
