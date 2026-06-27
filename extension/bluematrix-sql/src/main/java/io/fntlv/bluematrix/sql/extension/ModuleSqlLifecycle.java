package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.sql.core.BlueDatabase;
import io.fntlv.bluematrix.sql.core.BlueDatabaseSource;

final class ModuleSqlLifecycle {
    private final ModuleSqlRegistry sqlRegistry;
    private final SqlTableInitializer tableInitializer;

    ModuleSqlLifecycle(ModuleSqlRegistry sqlRegistry) {
        if (sqlRegistry == null) {
            throw new IllegalArgumentException("sqlRegistry cannot be null");
        }
        this.sqlRegistry = sqlRegistry;
        this.tableInitializer = new SqlTableInitializer();
    }

    void register(ModuleCandidate candidate) {
        if (BlueDatabaseSourceProvider.class.isAssignableFrom(candidate.getModuleClass())) {
            sqlRegistry.registerDatabase(candidate);
        }
    }

    void initialize(ModuleContext context) {
        if (!sqlRegistry.containsDatabase(context)) {
            return;
        }
        BlueDatabase database = sqlRegistry.getDatabase(context);
        BlueDatabaseSource source = databaseSource(context);
        database.initialize(source);
        tableInitializer.initialize(context, database);
    }

    void close(ModuleContext context) {
        if (!sqlRegistry.containsDatabase(context)) {
            return;
        }
        sqlRegistry.getDatabase(context).close();
    }

    private BlueDatabaseSource databaseSource(ModuleContext context) {
        Module module = context.getInstance();
        if (!(module instanceof BlueDatabaseSourceProvider)) {
            throw new IllegalStateException("Registered SQL module must implement BlueDatabaseSourceProvider: "
                    + context.getInfo().id() + " (" + module.getClass().getName() + ")");
        }
        return ((BlueDatabaseSourceProvider) module).getDatabaseSource();
    }
}
