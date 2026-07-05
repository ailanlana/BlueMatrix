package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityState;
import io.fntlv.bluematrix.persistence.core.cache.BlueCacheSyncCoordinator;
import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;
import io.fntlv.bluematrix.persistence.core.data.DefaultBlueDataAccess;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

final class ModulePersistenceState implements ModuleCapabilityState {
    private final BlueStorage storage;
    private final DefaultBlueDataAccess access;
    private final BlueCacheSyncCoordinator cacheSyncCoordinator;

    ModulePersistenceState() {
        this(new BlueStorage(), new BlueCacheSyncCoordinator());
    }

    ModulePersistenceState(BlueStorage storage) {
        this(storage, new BlueCacheSyncCoordinator());
    }

    ModulePersistenceState(BlueStorage storage, BlueCacheSyncCoordinator cacheSyncCoordinator) {
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        if (cacheSyncCoordinator == null) {
            throw new IllegalArgumentException("cacheSyncCoordinator cannot be null");
        }
        this.storage = storage;
        this.access = new DefaultBlueDataAccess(storage);
        this.cacheSyncCoordinator = cacheSyncCoordinator;
    }

    BlueStorage storage() {
        return storage;
    }

    BlueDataAccess dataAccess() {
        return access;
    }

    BlueDataQueryAccess queryAccess() {
        return access;
    }

    BlueCacheSyncCoordinator cacheSyncCoordinator() {
        return cacheSyncCoordinator;
    }
}
