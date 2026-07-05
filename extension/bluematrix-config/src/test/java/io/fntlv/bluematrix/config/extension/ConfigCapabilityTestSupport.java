package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityListener;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityRegistry;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;

import java.io.File;

public final class ConfigCapabilityTestSupport {
    private static final String CAPABILITY_ID = "config";

    private ConfigCapabilityTestSupport() {
    }

    public static ModuleConfigContext load(File dataFolder, ModuleContext context) {
        ModuleCapability<ModuleConfigContext, ModuleConfigState> capability = capability(dataFolder);
        ModuleCapabilityListener listener = listener(capability);
        listener.onRegisterPre(new io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent.Pre(candidate(context)));
        listener.onLoadPre(new ModuleLoadEvent.Pre(context));
        return capability.context(context.id());
    }

    public static ModuleCapability<ModuleConfigContext, ModuleConfigState> capability(File dataFolder) {
        return capability(dataFolder, new ConfigRegisterProcessor());
    }

    public static ModuleCapability<ModuleConfigContext, ModuleConfigState> capability(
            File dataFolder,
            ConfigRegisterProcessor processor) {
        ModuleConfigInitializer initializer = initializer(dataFolder, processor);
        return ModuleCapability.<ModuleConfigContext, ModuleConfigState>builder(CAPABILITY_ID)
                .contextType(ModuleConfigContext.class)
                .stateFactory(initializer::createState)
                .contextFactory(ModuleConfigContext::new)
                .onLoadPre((binding, event) -> {
                    try {
                        initializer.initialize(event.getContext(), binding.state());
                    } catch (RuntimeException e) {
                        event.error(CAPABILITY_ID, "Module configuration failed", e);
                    }
                })
                .onEnablePre((binding, event) -> {
                    if (!binding.state().moduleEnabled()) {
                        event.cancel(ModuleConditionOutcome.noMatch(
                                CAPABILITY_ID,
                                "Module disabled by configuration"
                        ));
                    }
                })
                .onDisablePost((binding, event) -> initializer.save(event.getContext(), binding.state()))
                .onDisableFailed((binding, event) -> initializer.save(event.getContext(), binding.state()))
                .build();
    }

    public static ModuleCapabilityListener listener(ModuleCapability<ModuleConfigContext, ModuleConfigState> capability) {
        return new ModuleCapabilityListener(registry(capability));
    }

    public static ModuleCapabilityRegistry registry(ModuleCapability<ModuleConfigContext, ModuleConfigState> capability) {
        ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
        registry.register(capability);
        return registry;
    }

    public static ModuleConfigInitializer initializer(File dataFolder) {
        return initializer(dataFolder, new ConfigRegisterProcessor());
    }

    public static ModuleConfigInitializer initializer(File dataFolder, ConfigRegisterProcessor processor) {
        return new ModuleConfigInitializer(dataFolder, new YamlConfigFileFormat(), processor);
    }

    public static ModuleConfigState state(File dataFolder, ModuleContext context) {
        return initializer(dataFolder).createState(context.id());
    }

    public static ConfigFile openFile(File dataFolder, String moduleId) {
        return initializer(dataFolder).openFile(moduleId);
    }

    public static ConfigFile openFile(File dataFolder, String moduleId, String fileName) {
        return initializer(dataFolder).openFile(moduleId, fileName);
    }

    public static File modulePath(File dataFolder, String moduleId) {
        return initializer(dataFolder).modulePath(moduleId);
    }

    private static ModuleCandidate candidate(ModuleContext context) {
        return new ModuleCandidate(context.getModuleClass(), context.getDescriptor());
    }
}
