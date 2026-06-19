package io.fntlv.bluematrix.config.extension;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.format.ConfigFileFormat;
import io.fntlv.bluematrix.config.extension.context.DefaultModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigContext;
import io.fntlv.bluematrix.config.extension.context.ModuleConfigState;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

public class ModuleConfigRegistry {
    public static final String MODULES_DIRECTORY_NAME = "modules";
    public static final String DEFAULT_FILE_NAME = "config.yml";

    private final File dataFolder;
    @Getter
    private final ConfigFileFormat fileFormat;
    private final Map<String, ModuleConfigContext> registeredContexts = Collections.synchronizedMap(new HashMap<>());

    public ModuleConfigRegistry(File dataFolder, ConfigFileFormat fileFormat) {
        this.dataFolder = dataFolder;
        this.fileFormat = fileFormat;
    }

    public ModuleConfigContext registerContext(ModuleCandidate candidate) {
        return registerContext(candidate.getModuleInfo().id());
    }

    public ModuleConfigContext getContext(ModuleCandidate candidate) {
        String moduleId = candidate.getModuleInfo().id();
        ModuleConfigContext context = registeredContexts.get(moduleId);
        if (context == null) {
            throw new IllegalStateException("ModuleConfigContext should be registered for every module. "
                    + "Missing context indicates an unexpected config extension lifecycle state: "
                    + moduleId + " (" + candidate.getModuleClass().getName() + ")");
        }
        return context;
    }

    public ModuleConfigContext getContext(ModuleContext context) {
        String moduleId = context.getInfo().id();
        ModuleConfigContext configContext = registeredContexts.get(moduleId);
        if (configContext == null) {
            throw new IllegalStateException("ModuleConfigContext should be registered for every module. "
                    + "Missing context indicates an unexpected config extension lifecycle state: "
                    + moduleId + " (" + context.getInstance().getClass().getName() + ")");
        }
        return configContext;
    }

    public void bindContext(ModuleContext context, ModuleConfigState state) {
        ModuleConfigContext configContext = registerContext(context.getInfo().id());
        if (!(configContext instanceof DefaultModuleConfigContext)) {
            throw new IllegalStateException("Unsupported module config context type: "
                    + configContext.getClass().getName());
        }
        ((DefaultModuleConfigContext) configContext).bindState(state);
    }

    public ConfigFile openFile(String moduleId) {
        return openFile(moduleId, DEFAULT_FILE_NAME);
    }

    public ConfigFile openFile(String moduleId, String fileName) {
        File modulePath = getModulePath(moduleId);
        File file = new File(modulePath, normalizeFileName(fileName));
        return fileFormat.open(file);
    }

    public ModuleConfigState getState(ModuleContext context) {
        return getState(getContext(context));
    }

    public ModuleConfigState getState(ModuleConfigContext context) {
        if (!(context instanceof DefaultModuleConfigContext)) {
            throw new IllegalStateException("Unsupported module config context type: "
                    + context.getClass().getName());
        }
        return ((DefaultModuleConfigContext) context).state();
    }

    public File getModulePath(String moduleId) {
        return new File(new File(dataFolder, MODULES_DIRECTORY_NAME), moduleId);
    }

    public static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return DEFAULT_FILE_NAME;
        }

        String normalized = fileName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Config file name cannot be blank");
        }
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains("..")) {
            throw new IllegalArgumentException("Config file name must be a simple file name: " + fileName);
        }
        if (normalized.endsWith(".yml")) {
            String baseName = normalized.substring(0, normalized.length() - ".yml".length());
            if (baseName.trim().isEmpty()) {
                throw new IllegalArgumentException("Config file name cannot be blank: " + fileName);
            }
            return normalized;
        }
        return normalized + ".yml";
    }

    private ModuleConfigContext registerContext(String moduleId) {
        synchronized (registeredContexts) {
            ModuleConfigContext context = registeredContexts.get(moduleId);
            if (context == null) {
                context = new DefaultModuleConfigContext(moduleId);
                registeredContexts.put(moduleId, context);
            }
            return context;
        }
    }

}
