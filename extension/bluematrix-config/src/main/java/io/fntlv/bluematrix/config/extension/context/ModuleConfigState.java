package io.fntlv.bluematrix.config.extension.context;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.extension.ModuleConfigFileNames;
import io.fntlv.bluematrix.config.extension.register.RegisteredConfig;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ModuleConfigState implements ModuleCapabilityState {
    private Module module;
    private final String moduleId;
    private final ModuleConfigFiles files;
    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private final List<RegisteredConfig> registeredConfigs = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean moduleEnabled = true;

    public ModuleConfigState(Module module, String moduleId, ConfigFile file) {
        this(module, moduleId, fileName -> {
            String normalized = file.getFile().getName();
            String requested = ModuleConfigFileNames.normalize(fileName);
            if (!normalized.equals(requested)) {
                throw new IllegalStateException("Module config state cannot open additional config file: " + fileName);
            }
            return file;
        });
    }

    public ModuleConfigState(String moduleId, Function<String, ConfigFile> fileResolver) {
        this(null, moduleId, fileResolver);
    }

    public ModuleConfigState(Module module, String moduleId, Function<String, ConfigFile> fileResolver) {
        this.module = module;
        this.moduleId = moduleId;
        this.files = new ModuleConfigFiles(fileResolver);
    }

    public <T> T get(Class<T> type) {
        if (!instances.containsKey(type)) {
            throw new IllegalStateException("Config type is not registered for module "
                    + moduleId + ": " + type.getName());
        }
        return type.cast(instances.get(type));
    }

    public ConfigFile file() {
        return files.file();
    }

    public ConfigFile file(String fileName) {
        return files.file(fileName);
    }

    public void saveFilesIfChanged() {
        files.saveIfChanged();
    }

    public String moduleId() {
        return moduleId;
    }

    public boolean moduleEnabled() {
        return moduleEnabled;
    }

    public void moduleEnabled(boolean moduleEnabled) {
        this.moduleEnabled = moduleEnabled;
    }

    public Module module() {
        if (module == null) {
            throw new IllegalStateException("Module instance is not bound for module: " + moduleId);
        }
        return module;
    }

    public synchronized void bind(Module module) {
        if (module == null) {
            throw new IllegalArgumentException("module cannot be null");
        }
        if (this.module != null && this.module != module) {
            throw new IllegalStateException("Module config state is already bound for module " + moduleId
                    + ": " + this.module.getClass().getName());
        }
        this.module = module;
    }

    public void register(RegisteredConfig config) {
        Object previous = instances.putIfAbsent(config.type(), config.instance());
        if (previous != null) {
            throw new IllegalStateException("Config type is already registered for module "
                    + moduleId + ": " + config.type().getName());
        }
        registeredConfigs.add(config);
    }

    public List<RegisteredConfig> registeredConfigs() {
        synchronized (registeredConfigs) {
            return Collections.unmodifiableList(new ArrayList<>(registeredConfigs));
        }
    }

}
