package io.fntlv.bluematrix.sql.extension;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.SQLQuery;
import cc.carm.lib.easysql.api.SQLTable;
import cc.carm.lib.easysql.api.builder.*;
import cc.carm.lib.easysql.api.function.SQLBiFunction;
import cc.carm.lib.easysql.api.function.SQLDebugHandler;
import cc.carm.lib.easysql.api.function.SQLExceptionHandler;
import io.fntlv.bluematrix.core.BlueMatrixContainerEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.instance.parameter.ModuleParameterResolverRegistry;
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

class SqlModuleListenerTest {

    @Test
    void containerCreatedAddsBlueDatabaseConstructorParameterResolver() {
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(SourceProviderModule.class);

        listener.onContainerCreated(containerCreated(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertEquals(1, parameterResolvers.resolvers().size());

        SourceProviderModule module = (SourceProviderModule) new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate);
        assertFalse(module.database.available());
        assertSame(module.database, registry.getDatabase(candidate));
        assertThrows(IllegalStateException.class, () -> module.database.sqlManager());
    }

    @Test
    void moduleWithoutSourceProviderDoesNotRegisterDatabase() {
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        ModuleCandidate candidate = candidate(PlainModule.class);
        PlainModule module = new PlainModule();
        ModuleContext context = context(module);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        assertFalse(registry.containsDatabase(candidate));

        assertDoesNotThrow(() -> listener.onEnablePre(new ModuleEnableEvent.Pre(context)));
        assertDoesNotThrow(() -> listener.onDisablePost(new ModuleDisableEvent.Post(context)));
        assertDoesNotThrow(() -> listener.onDisableFailed(new ModuleDisableEvent.Failed(context, new IllegalStateException("disable failed"))));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> registry.getDatabase(context));
        assertExceptionMessageContains(exception, "BlueDatabase should be registered for SQL-enabled modules");
        assertExceptionMessageContains(exception, "unexpected SQL extension lifecycle state");
        assertExceptionMessageContains(exception, "plain");
    }

    @Test
    void nonSourceProviderCannotInjectBlueDatabase() {
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(ConstructorSqlModule.class);

        listener.onContainerCreated(containerCreated(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertFalse(registry.containsDatabase(candidate));
        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class, () -> new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate));
        assertCauseMessageContains(exception,
                "BlueDatabase injection requires module to implement BlueDatabaseSourceProvider");
        assertCauseMessageContains(exception, "constructor-sql");
        assertCauseMessageContains(exception, ConstructorSqlModule.class.getName());
    }

    @Test
    void moduleWithSourceProviderInitializesInjectedDatabase() {
        FakeSqlManager manager = new FakeSqlManager();
        ModuleSqlRegistry registry = new TestModuleSqlRegistry(manager);
        SqlModuleListener listener = new SqlModuleListener(registry);
        SourceProviderModule module = createModule(listener, registry, SourceProviderModule.class);
        ModuleContext context = context(module);

        assertFalse(module.database.available());

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertTrue(module.database.available());
        assertSame(manager, module.database.sqlManager());
        assertSame(module.database, registry.getDatabase(context));
        assertEquals("127.0.0.1", module.source.getIp());
    }

    @Test
    void sourceProviderCanInjectBlueDatabaseField() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        FakeSqlManager manager = new FakeSqlManager();
        ModuleSqlRegistry registry = new TestModuleSqlRegistry(manager);
        SqlModuleListener listener = new SqlModuleListener(registry);
        FieldSqlModule module = createModule(listener, registry, FieldSqlModule.class);
        ModuleContext context = context(module);

        assertFalse(module.database.available());

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertSame(manager, module.database.sqlManager());
        assertSame(module.database, registry.getDatabase(context));
    }

    @Test
    void sourceProviderFailureReportsEnablePreError() {
        RuntimeException failure = new RuntimeException("database source failed");
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        ThrowingSourceProviderModule module = new ThrowingSourceProviderModule(failure);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(ThrowingSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertEquals("Module SQL initialization failed", event.getErrorMessage());
        assertSame(failure, event.getErrorCause());
    }

    @Test
    void nullSourceReportsEnablePreError() {
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        NullSourceProviderModule module = new NullSourceProviderModule();
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(NullSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertEquals("Module SQL initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
    }

    @Test
    void disableClosesInitializedDatabase() {
        FakeSqlManager manager = new FakeSqlManager();
        TestModuleSqlRegistry registry = new TestModuleSqlRegistry(manager);
        SqlModuleListener listener = new SqlModuleListener(registry);
        SourceProviderModule module = createModule(listener, registry, SourceProviderModule.class);
        ModuleContext context = context(module);

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        listener.onDisablePost(new ModuleDisableEvent.Post(context));
        listener.onDisableFailed(new ModuleDisableEvent.Failed(context, new IllegalStateException("disable failed")));

        assertEquals(2, ((RecordingBlueDatabase) registry.getDatabase(context)).closeCalls);
    }

    @Test
    void enablePreCreatesSqlTableEnumsForSourceProviderModule() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        FakeSqlManager manager = new FakeSqlManager();
        ModuleSqlRegistry registry = new TestModuleSqlRegistry(manager);
        SqlModuleListener listener = new SqlModuleListener(registry);
        SourceProviderModule module = createModule(listener, registry, SourceProviderModule.class);
        ModuleContext context = context(module);
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context);

        listener.onEnablePre(event);

        assertFalse(event.hasError());
        assertEquals(1, RecordingSqlTable.createCalls);
        assertSame(manager, RecordingSqlTable.sqlManager);
    }

    @Test
    void enablePreSkipsTableCreationForModulesWithoutRegisteredDatabase() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        ModuleSqlRegistry registry = new ModuleSqlRegistry();
        SqlModuleListener listener = new SqlModuleListener(registry);
        PlainModule module = new PlainModule();

        assertDoesNotThrow(() -> listener.onEnablePre(new ModuleEnableEvent.Pre(context(module))));

        assertEquals(0, RecordingSqlTable.createCalls);
    }

    @Test
    void sqlTableCreationFailureReportsEnablePreError() {
        RecordingSqlTable.reset();
        FailingSqlTable.reset();
        SQLException failure = new SQLException("create table failed");
        FailingSqlTable.failure = failure;
        FakeSqlManager manager = new FakeSqlManager();
        ModuleSqlRegistry registry = new TestModuleSqlRegistry(manager);
        SqlModuleListener listener = new SqlModuleListener(registry);
        SourceProviderModule module = createModule(listener, registry, SourceProviderModule.class);
        ModuleContext context = context(module);
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context);

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("sql", event.getErrorSource());
        assertEquals("Module SQL initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof SqlTableInitializationException);
        assertExceptionMessageContains(event.getErrorCause(), "Module SQL table initialization failed");
        assertExceptionMessageContains(event.getErrorCause(), "source-provider");
        assertSame(failure, event.getErrorCause().getCause());
        FailingSqlTable.reset();
    }

    private static ModuleCandidate candidate(Class<? extends Module> type) {
        return new ModuleCandidate(type, type.getAnnotation(ModuleInfo.class));
    }

    private static ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
    }

    private BlueMatrixContainerEvent.Created containerCreated(ModuleParameterResolverRegistry parameterResolvers) {
        return new BlueMatrixContainerEvent.Created(parameterResolvers, new io.fntlv.bluematrix.core.module.instance.DefaultModuleInstanceFactory(parameterResolvers));
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

    @SuppressWarnings("unchecked")
    private <T extends Module> T createModule(SqlModuleListener listener,
                                              ModuleSqlRegistry registry,
                                              Class<T> type) {
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(type);
        listener.onContainerCreated(containerCreated(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        return (T) new DefaultModuleInstanceFactory(parameterResolvers).create(candidate);
    }

    @ModuleInfo(id = "constructor-sql", name = "Constructor SQL")
    private static class ConstructorSqlModule implements Module {
        private final BlueDatabase database;

        private ConstructorSqlModule(BlueDatabase database) {
            this.database = database;
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
        private final BlueDatabase database;
        private final BlueDatabaseSource source = new TestSource();

        private SourceProviderModule(BlueDatabase database) {
            this.database = database;
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
        private BlueDatabase database;
        private final BlueDatabaseSource source = new TestSource();

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

    private static class TestModuleSqlRegistry extends ModuleSqlRegistry {
        private final SQLManager manager;

        private TestModuleSqlRegistry(SQLManager manager) {
            this.manager = manager;
        }

        @Override
        protected BlueDatabase createDatabase() {
            return new RecordingBlueDatabase(manager);
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
