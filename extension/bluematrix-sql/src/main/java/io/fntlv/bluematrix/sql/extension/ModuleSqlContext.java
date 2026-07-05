package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContext;
import io.fntlv.bluematrix.sql.core.BlueDatabase;

public final class ModuleSqlContext implements ModuleCapabilityContext {
    private final String moduleId;
    private final BlueDatabase database;

    ModuleSqlContext(String moduleId, BlueDatabase database) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be blank");
        }
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null");
        }
        this.moduleId = moduleId;
        this.database = database;
    }

    @Override
    public String moduleId() {
        return moduleId;
    }

    public BlueDatabase database() {
        return database;
    }
}
