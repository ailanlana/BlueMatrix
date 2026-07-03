package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;
import io.fntlv.bluematrix.persistence.core.data.DefaultBlueDataAccess;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

final class ModulePersistenceState {
    private final BlueStorage storage;
    private final DefaultBlueDataAccess access;

    ModulePersistenceState() {
        this(new BlueStorage());
    }

    ModulePersistenceState(BlueStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        this.storage = storage;
        this.access = new DefaultBlueDataAccess(storage);
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
}
