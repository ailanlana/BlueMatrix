package io.fntlv.bluematrix.persistence.extension;

import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContext;
import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;

public final class ModulePersistenceContext implements ModuleCapabilityContext {
    private final String moduleId;
    private final ModulePersistenceState state;

    ModulePersistenceContext(String moduleId, ModulePersistenceState state) {
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

    public BlueDataAccess dataAccess() {
        return state.dataAccess();
    }

    public BlueDataQueryAccess queryAccess() {
        return state.queryAccess();
    }
}
