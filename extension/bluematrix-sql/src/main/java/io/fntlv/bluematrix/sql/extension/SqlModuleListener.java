package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.BlueMatrixContainerEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;
import io.fntlv.bluematrix.sql.core.BlueDatabase;
import io.fntlv.bluematrix.sql.core.BlueDatabaseSource;

public class SqlModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(SqlModuleListener.class);

    private final ModuleSqlRegistry sqlRegistry;
    private final SqlTableInitializer tableInitializer;

    public SqlModuleListener(ModuleSqlRegistry sqlRegistry) {
        if (sqlRegistry == null) {
            throw new IllegalArgumentException("sqlRegistry cannot be null");
        }
        this.sqlRegistry = sqlRegistry;
        this.tableInitializer = new SqlTableInitializer();
    }

    @ModuleEventListener
    public void onContainerCreated(BlueMatrixContainerEvent.Created event) {
        event.getParameterResolvers().registerIfAbsent(new SqlDatabaseResolver(sqlRegistry));
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        if (BlueDatabaseSourceProvider.class.isAssignableFrom(event.getCandidate().getModuleClass())) {
            sqlRegistry.registerDatabase(event.getCandidate());
        }
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        ModuleContext context = event.getContext();
        Module module = context.getInstance();
        if (!sqlRegistry.containsDatabase(context)) {
            return;
        }
        try {
            BlueDatabase database = sqlRegistry.getDatabase(context);
            BlueDatabaseSource source = ((BlueDatabaseSourceProvider) module).getDatabaseSource();
            database.initialize(source);
            tableInitializer.initialize(context, database);
        } catch (RuntimeException e) {
            event.error("sql", "Module SQL initialization failed", e);
        }
    }

    @ModuleEventListener
    public void onDisablePost(ModuleDisableEvent.Post event) {
        close(event.getContext());
    }

    @ModuleEventListener
    public void onDisableFailed(ModuleDisableEvent.Failed event) {
        close(event.getContext());
    }

    private void close(ModuleContext context) {
        if (!sqlRegistry.containsDatabase(context)) {
            return;
        }
        try {
            sqlRegistry.getDatabase(context).close();
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module SQL shutdown failed during disable: [module=%s]",
                    context.getInfo().id()
            ), e);
        }
    }

}
