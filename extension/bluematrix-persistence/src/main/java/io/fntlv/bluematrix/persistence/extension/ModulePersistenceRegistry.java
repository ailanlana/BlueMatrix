package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.persistence.core.BlueStorage;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModulePersistenceRegistry {
    public static final String MODULES_DIRECTORY_NAME = "modules";

    private final File dataFolder;
    private final Map<String, BlueStorage> registeredStorages = Collections.synchronizedMap(new HashMap<String, BlueStorage>());

    public ModulePersistenceRegistry(File dataFolder) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        this.dataFolder = dataFolder;
    }

    public BlueStorage registerStorage(ModuleCandidate candidate) {
        return registerStorage(candidate.getModuleInfo().id());
    }

    public boolean containsStorage(ModuleCandidate candidate) {
        return registeredStorages.containsKey(candidate.getModuleInfo().id());
    }

    public boolean containsStorage(ModuleContext context) {
        return registeredStorages.containsKey(context.getInfo().id());
    }

    public BlueStorage getStorage(ModuleCandidate candidate) {
        return getStorage(candidate.getModuleInfo().id());
    }

    public BlueStorage getStorage(ModuleContext context) {
        return getStorage(context.getInfo().id());
    }

    public BlueStorage getStorage(String moduleId) {
        BlueStorage storage = registeredStorages.get(moduleId);
        if (storage == null) {
            throw new IllegalStateException(missingStorageMessage(moduleId));
        }
        return storage;
    }

    public File getModulePath(String moduleId) {
        return new File(new File(dataFolder, MODULES_DIRECTORY_NAME), moduleId);
    }

    public File getModuleDataPath(String moduleId) {
        return new File(getModulePath(moduleId), "data");
    }

    private BlueStorage registerStorage(String moduleId) {
        synchronized (registeredStorages) {
            BlueStorage storage = registeredStorages.get(moduleId);
            if (storage == null) {
                storage = createStorage();
                registeredStorages.put(moduleId, storage);
            }
            return storage;
        }
    }

    private String missingStorageMessage(String moduleId) {
        return "BlueStorage should be registered for persistence-enabled modules. "
                + "Missing storage indicates an unexpected persistence extension lifecycle state: " + moduleId;
    }

    protected BlueStorage createStorage() {
        return new BlueStorage();
    }
}
