package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.sql.core.BlueDatabase;
import io.fntlv.bluematrix.sql.core.BlueDatabaseSource;

import java.util.function.Supplier;

final class ModuleSqlInitializer {
    private final Supplier<BlueDatabase> databaseFactory;
    private final SqlTableInitializer tableInitializer;

    ModuleSqlInitializer() {
        this(BlueDatabase::new, new SqlTableInitializer());
    }

    ModuleSqlInitializer(Supplier<BlueDatabase> databaseFactory, SqlTableInitializer tableInitializer) {
        if (databaseFactory == null) {
            throw new IllegalArgumentException("databaseFactory cannot be null");
        }
        if (tableInitializer == null) {
            throw new IllegalArgumentException("tableInitializer cannot be null");
        }
        this.databaseFactory = databaseFactory;
        this.tableInitializer = tableInitializer;
    }

    ModuleSqlContext createContext(String moduleId) {
        BlueDatabase database = databaseFactory.get();
        if (database == null) {
            throw new IllegalStateException("SQL database factory cannot return null");
        }
        return new ModuleSqlContext(moduleId, database);
    }

    void initialize(ModuleContext context, ModuleSqlContext sqlContext) {
        BlueDatabase database = sqlContext.database();
        database.initialize(databaseSource(context));
        tableInitializer.initialize(context, database);
    }

    void close(ModuleSqlContext context) {
        context.database().close();
    }

    private BlueDatabaseSource databaseSource(ModuleContext context) {
        Module module = context.getInstance();
        if (!(module instanceof BlueDatabaseSourceProvider)) {
            throw new IllegalStateException("Registered SQL module must implement BlueDatabaseSourceProvider: "
                    + context.id() + " (" + module.getClass().getName() + ")");
        }
        return ((BlueDatabaseSourceProvider) module).getDatabaseSource();
    }
}
