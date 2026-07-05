package io.fntlv.bluematrix.sql.extension;

import io.fntlv.bluematrix.core.extension.BlueMatrixExtension;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionBootstrap;
import io.fntlv.bluematrix.core.extension.BlueMatrixExtensionContext;
import io.fntlv.bluematrix.core.module.capability.EmptyModuleCapabilityState;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityBinding;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

public final class SqlExtension implements BlueMatrixExtension {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(SqlExtension.class);
    private static final String CAPABILITY_ID = "sql";

    @Override
    public void apply(BlueMatrixExtensionBootstrap bootstrap, BlueMatrixExtensionContext context) {
        ModuleSqlInitializer initializer = new ModuleSqlInitializer();
        ModuleCapability<ModuleSqlContext, ?> sqlCapability =
                ModuleCapability.<ModuleSqlContext, EmptyModuleCapabilityState>builder(CAPABILITY_ID)
                        .contextType(ModuleSqlContext.class)
                        .enabledWhen(candidate -> BlueDatabaseSourceProvider.class
                                .isAssignableFrom(candidate.getModuleClass()))
                        .contextFactory((moduleId, state) -> initializer.createContext(moduleId))
                        .onEnablePre((binding, event) -> initializeSql(initializer, binding, event))
                        .onDisablePost((binding, event) -> closeSql(initializer, binding, event))
                        .onDisableFailed((binding, event) -> closeSql(initializer, binding, event))
                        .build();
        bootstrap.repository("https://repo.carm.cc/repository/maven-public/")
                .extensionLibrary(
                        context.getName(),
                        "cc.carm.lib:easysql-api:0.4.7",
                        "cc.carm.lib.easysql.api.SQLManager"
                )
                .extensionLibrary(
                        context.getName(),
                        "cc.carm.lib:easysql-hikaricp:0.4.7",
                        "cc.carm.lib.easysql.EasySQL"
                )
                .extensionLibrary(
                        context.getName(),
                        "com.mysql:mysql-connector-j:9.4.0",
                        "com.mysql.cj.jdbc.Driver"
                )
                .moduleCapability(sqlCapability);
    }

    private static void initializeSql(ModuleSqlInitializer initializer,
                                      ModuleCapabilityBinding<ModuleSqlContext, ?> binding,
                                      ModuleEnableEvent.Pre event) {
        try {
            initializer.initialize(event.getContext(), binding.context());
        } catch (RuntimeException e) {
            event.error(CAPABILITY_ID, "Module SQL initialization failed", e);
        }
    }

    private static void closeSql(ModuleSqlInitializer initializer,
                                 ModuleCapabilityBinding<ModuleSqlContext, ?> binding,
                                 ModuleDisableEvent event) {
        try {
            initializer.close(binding.context());
        } catch (RuntimeException e) {
            LOGGER.error(String.format(
                    "Module SQL shutdown failed during disable: [module=%s]",
                    event.getContext().id()
            ), e);
        }
    }
}
