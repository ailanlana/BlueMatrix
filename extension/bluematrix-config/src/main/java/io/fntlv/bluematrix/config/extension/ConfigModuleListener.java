package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.ModuleConditionOutcome;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleLoadEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConfigModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(ConfigModuleListener.class);

    private final ModuleConfigRegistry configRegistry;
    private final ConfigRegisterProcessor configRegisterProcessor;
    private final Set<DisabledModule> disabledModules = new HashSet<>();

    public ConfigModuleListener(File dataFolder) {
        this(new ModuleConfigRegistry(dataFolder, new YamlConfigFileFormat()));
    }

    public ConfigModuleListener(ModuleConfigRegistry configRegistry) {
        this(configRegistry, new ConfigRegisterProcessor());
    }

    public ConfigModuleListener(ModuleConfigRegistry configRegistry, ConfigRegisterProcessor configRegisterProcessor) {
        this.configRegistry = configRegistry;
        this.configRegisterProcessor = configRegisterProcessor;
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        configRegistry.registerContext(event.getCandidate());
    }

    @ModuleEventListener
    public void onLoadPre(ModuleLoadEvent.Pre event) {
        ModuleContext context = event.getContext();
        String moduleId = context.getInfo().id();
        try {
            ModuleConfigState configState = new ModuleConfigState(
                    context.getInstance(),
                    moduleId,
                    fileName -> configRegistry.openFile(moduleId, fileName)
            );
            configRegistry.bindContext(context, configState);
            ConfigFile file = configState.file();

            String MODULE_ENABLE_PATH = "general.enable";
            boolean finalModuleEnable = file.getOrSetDefault(
                    MODULE_ENABLE_PATH,
                    context.getInfo().enableByDefault(),
                    "Whether to enable this module.\nSet to true to enable the module; false to skip enabling."
            );
            String DEBUG_ENABLE_PATH = "general.debug.enable";
            boolean debugEnabled = file.getOrSetDefault(
                    DEBUG_ENABLE_PATH,
                    false,
                    "Whether to enable debug logs for this module."
            );
            BlueLoggerFactory.setDebugEnabled(debugEnabled);

            if (!finalModuleEnable) {
                disabledModules.add(DisabledModule.of(moduleId));
            } else {
                disabledModules.remove(DisabledModule.of(moduleId));
            }

            configRegisterProcessor.process(context, configState);

            configState.saveFilesIfChanged();
        } catch (RuntimeException e) {
            disabledModules.remove(DisabledModule.of(moduleId));
            event.error("config", "Module configuration failed", e);
        }
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        String moduleId = event.getContext().getInfo().id();
        if (disabledModules.contains(DisabledModule.of(moduleId))) {
            event.cancel(ModuleConditionOutcome.noMatch("config", "Module disabled by configuration"));
        }
    }

    @ModuleEventListener
    public void onEnablePost(ModuleEnableEvent.Post event) {
        clearModuleState(event.getContext().getInfo().id());
    }

    @ModuleEventListener
    public void onEnableSkipped(ModuleEnableEvent.Skipped event) {
        clearModuleState(event.getContext().getInfo().id());
    }

    @ModuleEventListener
    public void onEnableFailed(ModuleEnableEvent.Failed event) {
        clearModuleState(event.getContext().getInfo().id());
    }

    @ModuleEventListener
    public void onDisablePost(ModuleDisableEvent.Post event) {
        saveConfig(event.getContext());
    }

    @ModuleEventListener
    public void onDisableFailed(ModuleDisableEvent.Failed event) {
        saveConfig(event.getContext());
    }

    private void saveConfig(ModuleContext context) {
        String moduleId = context.getInfo().id();
        try {
            ModuleConfigState configState = configRegistry.getState(context);
            configRegisterProcessor.save(configState);
            configState.saveFilesIfChanged();
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module configuration save failed during disable: [module=%s]",
                    moduleId
            ), e);
        }
    }

    private void clearModuleState(String moduleId) {
        disabledModules.remove(DisabledModule.of(moduleId));
    }

    private static final class DisabledModule {
        private final String moduleId;

        private DisabledModule(String moduleId) {
            if (moduleId == null || moduleId.trim().isEmpty()) {
                throw new IllegalArgumentException("moduleId cannot be blank");
            }
            this.moduleId = moduleId;
        }

        private static DisabledModule of(String moduleId) {
            return new DisabledModule(moduleId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DisabledModule)) {
                return false;
            }
            DisabledModule that = (DisabledModule) o;
            return Objects.equals(moduleId, that.moduleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleId);
        }
    }
}
