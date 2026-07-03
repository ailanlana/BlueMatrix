package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

final class DefaultModulePersistenceContext implements ModulePersistenceContext {
    private final String moduleId;
    private final ModulePersistenceState state;

    DefaultModulePersistenceContext(String moduleId) {
        this(moduleId, new ModulePersistenceState());
    }

    DefaultModulePersistenceContext(String moduleId, ModulePersistenceState state) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.moduleId = moduleId;
        this.state = state;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    @Override
    public BlueDataAccess dataAccess() {
        return state.dataAccess();
    }

    @Override
    public BlueDataQueryAccess queryAccess() {
        return state.queryAccess();
    }

    ModulePersistenceState state() {
        return state;
    }

    BlueStorage storage() {
        return state.storage();
    }
}
