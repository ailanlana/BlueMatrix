package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.config.extension.register.ConfigRegisterProcessor;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.io.File;

final class ModuleConfigInitializer {
    static final String MODULES_DIRECTORY_NAME = "modules";

    private static final String MODULE_ENABLE_PATH = "general.enable";
    private static final String DEBUG_ENABLE_PATH = "general.debug.enable";

    private final File dataFolder;
    private final ConfigFileFormat fileFormat;
    private final ConfigRegisterProcessor configRegisterProcessor;

    ModuleConfigInitializer(File dataFolder,
                            ConfigFileFormat fileFormat,
                            ConfigRegisterProcessor configRegisterProcessor) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (fileFormat == null) {
            throw new IllegalArgumentException("fileFormat cannot be null");
        }
        if (configRegisterProcessor == null) {
            throw new IllegalArgumentException("configRegisterProcessor cannot be null");
        }
        this.dataFolder = dataFolder;
        this.fileFormat = fileFormat;
        this.configRegisterProcessor = configRegisterProcessor;
    }

    void initialize(ModuleContext context, ModuleConfigState state) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        state.bind(context.getInstance());

        boolean moduleEnabled = loadGeneralConfig(context, state.file());
        configRegisterProcessor.process(context, state);
        state.moduleEnabled(moduleEnabled);
        state.saveFilesIfChanged();
    }

    void save(ModuleContext context, ModuleConfigState state) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        configRegisterProcessor.save(state);
        state.saveFilesIfChanged();
    }

    ModuleConfigState createState(String moduleId) {
        return new ModuleConfigState(moduleId, fileName -> openFile(moduleId, fileName));
    }

    File modulePath(String moduleId) {
        return new File(new File(dataFolder, MODULES_DIRECTORY_NAME), moduleId);
    }

    ConfigFile openFile(String moduleId) {
        return openFile(moduleId, ModuleConfigFileNames.DEFAULT_FILE_NAME);
    }

    ConfigFile openFile(String moduleId, String fileName) {
        File file = new File(modulePath(moduleId), ModuleConfigFileNames.normalize(fileName));
        return fileFormat.open(file);
    }

    ConfigFileFormat fileFormat() {
        return fileFormat;
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
