package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.event.ModuleEventListener;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public class SqlModuleListener {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(SqlModuleListener.class);

    private final ModuleSqlLifecycle sqlLifecycle;

    public SqlModuleListener(ModuleSqlRegistry sqlRegistry) {
        if (sqlRegistry == null) {
            throw new IllegalArgumentException("sqlRegistry cannot be null");
        }
        this.sqlLifecycle = new ModuleSqlLifecycle(sqlRegistry);
    }

    @ModuleEventListener
    public void onRegisterPre(ModuleRegisterEvent.Pre event) {
        sqlLifecycle.register(event.getCandidate());
    }

    @ModuleEventListener
    public void onEnablePre(ModuleEnableEvent.Pre event) {
        ModuleContext context = event.getContext();
        try {
            sqlLifecycle.initialize(context);
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
        try {
            sqlLifecycle.close(context);
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module SQL shutdown failed during disable: [module=%s]",
                    context.id()
            ), e);
        }
    }

}
