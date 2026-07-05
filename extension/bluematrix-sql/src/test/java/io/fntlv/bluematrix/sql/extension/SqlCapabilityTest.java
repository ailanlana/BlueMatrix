package io.fntlv.bluematrix.sql.extension;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.SQLQuery;
import cc.carm.lib.easysql.api.SQLTable;
import cc.carm.lib.easysql.api.builder.*;
import cc.carm.lib.easysql.api.function.SQLBiFunction;
import cc.carm.lib.easysql.api.function.SQLDebugHandler;
import cc.carm.lib.easysql.api.function.SQLExceptionHandler;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.capability.EmptyModuleCapabilityState;
import io.fntlv.bluematrix.core.module.capability.ModuleCapability;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityContextResolver;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityListener;
import io.fntlv.bluematrix.core.module.capability.ModuleCapabilityRegistry;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.sql.core.BlueDatabase;
import io.fntlv.bluematrix.sql.core.BlueDatabaseSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlCapabilityTest {

    @Test
    void resolverInjectsModuleSqlContextConstructorParameter() {
        Fixture fixture = fixture(new FakeSqlManager());
        ModuleCandidate candidate = candidate(ContextSqlModule.class);

        fixture.register(candidate);
        ContextSqlModule module = (ContextSqlModule) new DefaultModuleInstanceFactory(fixture.parameterResolvers)
                .create(candidate);

        assertSame(module.sqlContext, fixture.capability.context(candidate.id()));
        assertSame(module.sqlContext.database(), fixture.capability.context(candidate.id()).database());
    }

    @Test
    void nonSourceProviderDoesNotCreateSqlBinding() {
        Fixture fixture = fixture(new FakeSqlManager());
        ModuleCandidate candidate = candidate(PlainModule.class);
        ModuleContext context = context(new PlainModule());

        fixture.register(candidate);
        fixture.listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        fixture.listener.onDisablePost(new ModuleDisableEvent.Post(context));

        assertFalse(fixture.capability.contains(candidate.id()));
    }

    @Test
    void nonSourceProviderCannotInjectModuleSqlContext() {
        Fixture fixture = fixture(new FakeSqlManager());
        ModuleCandidate candidate = candidate(ConstructorSqlModule.class);

        fixture.register(candidate);

        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class,
                () -> new DefaultModuleInstanceFactory(fixture.parameterResolvers).create(candidate));
        assertCauseMessageContains(exception,
                "Module capability context is not registered for module");
        assertCauseMessageContains(exception, ModuleSqlContext.class.getName());
        assertCauseMessageContains(exception, "constructor-sql");
    }

    @Test
    void enablePreInitializesDatabaseAndTables() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        FakeSqlManager manager = new FakeSqlManager();
        Fixture fixture = fixture(manager);
        SourceProviderModule module = fixture.create(SourceProviderModule.class);
        ModuleContext context = context(module);

        fixture.listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertTrue(module.sqlContext.database().available());
        assertSame(manager, module.sqlContext.database().sqlManager());
        assertEquals(1, RecordingSqlTable.createCalls);
        assertSame(manager, RecordingSqlTable.sqlManager);
    }

    @Test
    void sourceProviderCanInjectModuleSqlContextField() {
        FakeSqlManager manager = new FakeSqlManager();
        Fixture fixture = fixture(manager);
        FieldSqlModule module = fixture.create(FieldSqlModule.class);

        fixture.listener.onEnablePre(new ModuleEnableEvent.Pre(context(module)));

        assertSame(manager, module.sqlContext.database().sqlManager());
    }

    @Test
    void sourceProviderFailureReportsEnablePreError() {
        RuntimeException failure = new RuntimeException("database source failed");
        Fixture fixture = fixture(new FakeSqlManager());
        ThrowingSourceProviderModule module = new ThrowingSourceProviderModule(failure);
        ModuleContext context = context(module);
        fixture.register(candidate(ThrowingSourceProviderModule.class));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context);

        assertDoesNotThrow(() -> fixture.listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertEquals("Module SQL initialization failed", event.getErrorMessage());
        assertSame(failure, event.getErrorCause());
    }

    @Test
    void nullSourceReportsEnablePreError() {
        Fixture fixture = fixture(new FakeSqlManager());
        NullSourceProviderModule module = new NullSourceProviderModule();
        fixture.register(candidate(NullSourceProviderModule.class));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> fixture.listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
    }

    @Test
    void sqlTableCreationFailureReportsEnablePreError() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        SQLException failure = new SQLException("create table failed");
        FailingSqlTable.failure = failure;
        Fixture fixture = fixture(new FakeSqlManager());
        SourceProviderModule module = fixture.create(SourceProviderModule.class);
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> fixture.listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertEquals("Module SQL initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof SqlTableInitializationException);
        assertExceptionMessageContains(event.getErrorCause(), "Module SQL table initialization failed");
        assertExceptionMessageContains(event.getErrorCause(), "source-provider");
        assertSame(failure, event.getErrorCause().getCause());
        FailingSqlTable.reset();
    }

    @Test
    void disableClosesDatabaseAndRemovesBinding() {
        Fixture fixture = fixture(new FakeSqlManager());
        SourceProviderModule module = fixture.create(SourceProviderModule.class);
        ModuleContext context = context(module);

        fixture.listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        fixture.listener.onDisablePost(new ModuleDisableEvent.Post(context));

        assertEquals(1, ((RecordingBlueDatabase) module.sqlContext.database()).closeCalls);
        assertFalse(fixture.capability.contains(context.id()));
    }

    private static Fixture fixture(SQLManager manager) {
        ModuleSqlInitializer initializer = new ModuleSqlInitializer(
                () -> new RecordingBlueDatabase(manager),
                new SqlTableInitializer()
        );
        ModuleCapability<ModuleSqlContext, EmptyModuleCapabilityState> capability =
                ModuleCapability.<ModuleSqlContext, EmptyModuleCapabilityState>builder("sql")
                        .contextType(ModuleSqlContext.class)
                        .enabledWhen(candidate -> BlueDatabaseSourceProvider.class
                                .isAssignableFrom(candidate.getModuleClass()))
                        .contextFactory((moduleId, state) -> initializer.createContext(moduleId))
                        .onEnablePre((binding, event) -> {
                            try {
                                initializer.initialize(event.getContext(), binding.context());
                            } catch (RuntimeException e) {
                                event.error("sql", "Module SQL initialization failed", e);
                            }
                        })
                        .onDisablePost((binding, event) -> initializer.close(binding.context()))
                        .onDisableFailed((binding, event) -> initializer.close(binding.context()))
                        .build();
        return new Fixture(capability);
    }

    private static ModuleCandidate candidate(Class<? extends Module> type) {
        return new ModuleCandidate(type, type.getAnnotation(ModuleInfo.class));
    }

    private static ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
    }

    private static void assertCauseMessageContains(Throwable throwable, String expected) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return;
            }
            current = current.getCause();
        }
        throw new AssertionError("Expected cause message to contain: " + expected);
    }

    private static void assertExceptionMessageContains(Throwable throwable, String expected) {
        if (throwable.getMessage() == null || !throwable.getMessage().contains(expected)) {
            throw new AssertionError("Expected exception message to contain: " + expected);
        }
    }

    private static final class Fixture {
        private final ModuleCapability<ModuleSqlContext, EmptyModuleCapabilityState> capability;
        private final ModuleCapabilityListener listener;
        private final ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();

        private Fixture(ModuleCapability<ModuleSqlContext, EmptyModuleCapabilityState> capability) {
            this.capability = capability;
            ModuleCapabilityRegistry registry = new ModuleCapabilityRegistry();
            registry.register(capability);
            this.listener = new ModuleCapabilityListener(registry);
            parameterResolvers.registerIfAbsent(new ModuleCapabilityContextResolver(registry));
        }

        private void register(ModuleCandidate candidate) {
            listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        }

        @SuppressWarnings("unchecked")
        private <T extends Module> T create(Class<T> type) {
            ModuleCandidate candidate = candidate(type);
            register(candidate);
            return (T) new DefaultModuleInstanceFactory(parameterResolvers).create(candidate);
        }
    }

    @ModuleInfo(id = "constructor-sql", name = "Constructor SQL")
    private static class ConstructorSqlModule implements Module {
        @SuppressWarnings("unused")
        private final ModuleSqlContext sqlContext;

        private ConstructorSqlModule(ModuleSqlContext sqlContext) {
            this.sqlContext = sqlContext;
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "context-sql", name = "Context SQL")
    private static class ContextSqlModule implements Module, BlueDatabaseSourceProvider {
        private final ModuleSqlContext sqlContext;

        private ContextSqlModule(ModuleSqlContext sqlContext) {
            this.sqlContext = sqlContext;
        }

        @Override
        public BlueDatabaseSource getDatabaseSource() {
            return new TestSource();
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "plain", name = "Plain")
    private static class PlainModule implements Module {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "source-provider", name = "Source Provider")
    private static class SourceProviderModule implements Module, BlueDatabaseSourceProvider {
        private final ModuleSqlContext sqlContext;
        private final BlueDatabaseSource source = new TestSource();

        private SourceProviderModule(ModuleSqlContext sqlContext) {
            this.sqlContext = sqlContext;
        }

        @Override
        public BlueDatabaseSource getDatabaseSource() {
            return source;
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "field-sql", name = "Field SQL")
    private static class FieldSqlModule implements Module, BlueDatabaseSourceProvider {
        @ModuleInject
        private ModuleSqlContext sqlContext;

        @Override
        public BlueDatabaseSource getDatabaseSource() {
            return new TestSource();
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "throwing-source-provider", name = "Throwing Source Provider")
    private static class ThrowingSourceProviderModule implements Module, BlueDatabaseSourceProvider {
        private final RuntimeException failure;

        private ThrowingSourceProviderModule(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public BlueDatabaseSource getDatabaseSource() {
            throw failure;
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    @ModuleInfo(id = "null-source-provider", name = "Null Source Provider")
    private static class NullSourceProviderModule implements Module, BlueDatabaseSourceProvider {
        @Override
        public BlueDatabaseSource getDatabaseSource() {
            return null;
        }

        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }

    private static class TestSource implements BlueDatabaseSource {
        @Override
        public String getIp() {
            return "127.0.0.1";
        }

        @Override
        public int getPort() {
            return 3306;
        }

        @Override
        public String getDatabase() {
            return "minecraft";
        }

        @Override
        public String getUsername() {
            return "root";
        }

        @Override
        public String getPassword() {
            return "";
        }
    }

    private enum RecordingSqlTable implements SQLTable {
        USERS;

        private static int createCalls;
        private static SQLManager sqlManager;

        private static void reset() {
            createCalls = 0;
            sqlManager = null;
        }

        @Override
        public boolean create(SQLManager sqlManager) {
            createCalls++;
            RecordingSqlTable.sqlManager = sqlManager;
            return true;
        }

        @Override
        public SQLManager getSQLManager() {
            return sqlManager;
        }

        @Override
        public String getTableName() {
            return "users";
        }
    }

    private enum FailingSqlTable implements SQLTable {
        FAILING;

        private static SQLException failure;

        private static void reset() {
            failure = null;
        }

        @Override
        public boolean create(SQLManager sqlManager) throws SQLException {
            if (failure != null) {
                throw failure;
            }
            return true;
        }

        @Override
        public SQLManager getSQLManager() {
            return null;
        }

        @Override
        public String getTableName() {
            return "failing";
        }
    }

    private static class RecordingBlueDatabase extends BlueDatabase {
        private final SQLManager manager;
        private int closeCalls;

        private RecordingBlueDatabase(SQLManager manager) {
            this.manager = manager;
        }

        @Override
        protected SQLManager createSqlManager(BlueDatabaseSource source) {
            return manager;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }

    private static class FakeSqlManager implements SQLManager {
        @Override
        public Logger getLogger() {
            return null;
        }

        @Override
        public boolean isDebugMode() {
            return false;
        }

        @Override
        public ExecutorService getExecutorPool() {
            return null;
        }

        @Override
        public void setExecutorPool(ExecutorService executorPool) {
        }

        @Override
        public void setDebugMode(Supplier<Boolean> debugMode) {
        }

        @Override
        public SQLDebugHandler getDebugHandler() {
            return null;
        }

        @Override
        public void setDebugHandler(SQLDebugHandler debugHandler) {
        }

        @Override
        public DataSource getDataSource() {
            return null;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return null;
        }

        @Override
        public Map<UUID, SQLQuery> getActiveQuery() {
            return Collections.emptyMap();
        }

        @Override
        public SQLExceptionHandler getExceptionHandler() {
            return null;
        }

        @Override
        public void setExceptionHandler(SQLExceptionHandler handler) {
        }

        @Override
        public Integer executeSQL(String sql) {
            return null;
        }

        @Override
        public Integer executeSQL(String sql, Object[] params) {
            return null;
        }

        @Override
        public List<Integer> executeSQLBatch(String sql, Iterable<Object[]> paramsBatch) {
            return null;
        }

        @Override
        public List<Integer> executeSQLBatch(String sql, String... moreSQL) {
            return null;
        }

        @Override
        public List<Integer> executeSQLBatch(Iterable<String> sqlBatch) {
            return null;
        }

        @Override
        public <R> CompletableFuture<R> fetchMetadata(SQLBiFunction<DatabaseMetaData, Connection, R> reader) {
            return null;
        }

        @Override
        public <R> CompletableFuture<R> fetchMetadata(SQLBiFunction<DatabaseMetaData, Connection, ResultSet> supplier,
                                                       cc.carm.lib.easysql.api.function.SQLFunction<ResultSet, R> reader) {
            return null;
        }

        @Override
        public TableCreateBuilder createTable(String tableName) {
            return null;
        }

        @Override
        public TableAlterBuilder alterTable(String tableName) {
            return null;
        }

        @Override
        public TableMetadataBuilder fetchTableMetadata(String tablePattern) {
            return null;
        }

        @Override
        public QueryBuilder createQuery() {
            return null;
        }

        @Override
        public InsertBuilder<cc.carm.lib.easysql.api.action.PreparedSQLUpdateAction<Integer>> createInsert(String tableName) {
            return null;
        }

        @Override
        public InsertBuilder<cc.carm.lib.easysql.api.action.PreparedSQLUpdateBatchAction<Integer>> createInsertBatch(String tableName) {
            return null;
        }

        @Override
        public ReplaceBuilder<cc.carm.lib.easysql.api.action.PreparedSQLUpdateAction<Integer>> createReplace(String tableName) {
            return null;
        }

        @Override
        public ReplaceBuilder<cc.carm.lib.easysql.api.action.PreparedSQLUpdateBatchAction<Integer>> createReplaceBatch(String tableName) {
            return null;
        }

        @Override
        public UpdateBuilder createUpdate(String tableName) {
            return null;
        }

        @Override
        public DeleteBuilder createDelete(String tableName) {
            return null;
        }
    }
}
