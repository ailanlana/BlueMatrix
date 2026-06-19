package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.sql.core.BlueDatabase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModuleSqlRegistry {
    private final Map<String, BlueDatabase> registeredDatabases = Collections.synchronizedMap(new HashMap<>());

    public BlueDatabase registerDatabase(ModuleCandidate candidate) {
        return registerDatabase(candidate.getModuleInfo().id());
    }

    public boolean containsDatabase(ModuleCandidate candidate) {
        return registeredDatabases.containsKey(candidate.getModuleInfo().id());
    }

    public boolean containsDatabase(ModuleContext context) {
        return registeredDatabases.containsKey(context.getInfo().id());
    }

    public BlueDatabase getDatabase(ModuleCandidate candidate) {
        return getDatabase(candidate.getModuleInfo().id());
    }

    public BlueDatabase getDatabase(ModuleContext context) {
        return getDatabase(context.getInfo().id());
    }

    public BlueDatabase getDatabase(String moduleId) {
        BlueDatabase database = registeredDatabases.get(moduleId);
        if (database == null) {
            throw new IllegalStateException(missingDatabaseMessage(moduleId));
        }
        return database;
    }

    private BlueDatabase registerDatabase(String moduleId) {
        synchronized (registeredDatabases) {
            BlueDatabase database = registeredDatabases.get(moduleId);
            if (database == null) {
                database = createDatabase();
                registeredDatabases.put(moduleId, database);
            }
            return database;
        }
    }

    private String missingDatabaseMessage(String moduleId) {
        return "BlueDatabase should be registered for SQL-enabled modules. "
                + "Missing database indicates an unexpected SQL extension lifecycle state: " + moduleId;
    }

    protected BlueDatabase createDatabase() {
        return new BlueDatabase();
    }
}
