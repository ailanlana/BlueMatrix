package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

final class ModuleConfigLifecycle {
    private static final String MODULE_ENABLE_PATH = "general.enable";
    private static final String DEBUG_ENABLE_PATH = "general.debug.enable";

    private final ModuleConfigRegistry configRegistry;
    private final ConfigRegisterProcessor configRegisterProcessor;

    ModuleConfigLifecycle(ModuleConfigRegistry configRegistry, ConfigRegisterProcessor configRegisterProcessor) {
        if (configRegistry == null) {
            throw new IllegalArgumentException("configRegistry cannot be null");
        }
        if (configRegisterProcessor == null) {
            throw new IllegalArgumentException("configRegisterProcessor cannot be null");
        }
        this.configRegistry = configRegistry;
        this.configRegisterProcessor = configRegisterProcessor;
    }

    ModuleConfigLoadResult load(ModuleContext context) {
        String moduleId = context.id();
        ModuleConfigState configState = new ModuleConfigState(
                context.getInstance(),
                moduleId,
                fileName -> configRegistry.openFile(moduleId, fileName)
        );
        configRegistry.bindContext(context, configState);

        boolean moduleEnabled = loadGeneralConfig(context, configState.file());
        configRegisterProcessor.process(context, configState);
        configState.saveFilesIfChanged();

        return moduleEnabled ? ModuleConfigLoadResult.ENABLED : ModuleConfigLoadResult.DISABLED;
    }

    void save(ModuleContext context) {
        ModuleConfigState configState = configRegistry.getState(context);
        configRegisterProcessor.save(configState);
        configState.saveFilesIfChanged();
    }

    private boolean loadGeneralConfig(ModuleContext context, ConfigFile file) {
        boolean moduleEnabled = file.getOrSetDefault(
                MODULE_ENABLE_PATH,
                context.enableByDefault(),
                "Whether to enable this module.\nSet to true to enable the module; false to skip enabling."
        );
        boolean debugEnabled = file.getOrSetDefault(
                DEBUG_ENABLE_PATH,
                false,
                "Whether to enable debug logs for this module."
        );
        BlueLoggerFactory.setDebugEnabled(debugEnabled);
        return moduleEnabled;
    }
}
