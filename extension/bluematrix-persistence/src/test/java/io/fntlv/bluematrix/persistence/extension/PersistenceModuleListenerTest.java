package io.fntlv.bluematrix.persistence.extension;

import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.Storage;
import br.com.finalcraft.everydatabase.modules.localfile.LocalFileConfig;
import br.com.finalcraft.everydatabase.modules.memory.InMemoryConfig;
import br.com.finalcraft.everydatabase.modules.mongo.MongoConfig;
import br.com.finalcraft.everydatabase.modules.sql.SqlConfig;
import br.com.finalcraft.everydatabase.modules.sql.PoolTuning;
import io.fntlv.bluematrix.core.BlueMatrixContainerEvent;
import io.fntlv.bluematrix.core.module.Module;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.core.module.ModuleInfo;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleDisableEvent;
import io.fntlv.bluematrix.core.module.lifecycle.event.ModuleEnableEvent;
import io.fntlv.bluematrix.core.module.registration.ModuleCandidate;
import io.fntlv.bluematrix.core.module.registration.ModuleRegisterEvent;
import io.fntlv.bluematrix.core.module.registration.exception.ModuleInstantiationException;
import io.fntlv.bluematrix.core.module.registration.instance.DefaultModuleInstanceFactory;
import io.fntlv.bluematrix.core.module.registration.instance.inject.ModuleInject;
import io.fntlv.bluematrix.core.module.registration.instance.parameter.ModuleParameterResolverRegistry;
import io.fntlv.bluematrix.persistence.core.BlueStorage;
import io.fntlv.bluematrix.persistence.core.BlueStorageSpec;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import io.fntlv.bluematrix.persistence.core.sources.BlueInMemoryStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueLocalFileStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueMongoStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueSqlStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueSqlType;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSource;
import io.fntlv.bluematrix.persistence.core.sources.BlueStorageSourceContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceModuleListenerTest {
    private static final File DATA_FOLDER = new File("build/test-data/persistence");

    @Test
    void containerCreatedAddsBlueStorageConstructorParameterResolver() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(StorageProviderModule.class);

        listener.onContainerCreated(new BlueMatrixContainerEvent.Created(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertEquals(1, parameterResolvers.resolvers().size());

        StorageProviderModule module = (StorageProviderModule) new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate);
        assertFalse(module.storage.available());
        assertSame(module.storage, registry.getStorage(candidate));
        assertThrows(IllegalStateException.class, () -> module.storage.storage());
    }

    @Test
    void moduleWithoutSourceProviderDoesNotRegisterStorage() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        ModuleCandidate candidate = candidate(PlainModule.class);
        PlainModule module = new PlainModule();
        ModuleContext context = context(module);

        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        assertFalse(registry.containsStorage(candidate));

        assertDoesNotThrow(() -> listener.onEnablePre(new ModuleEnableEvent.Pre(context)));
        assertDoesNotThrow(() -> listener.onDisablePost(new ModuleDisableEvent.Post(context)));
        assertDoesNotThrow(() -> listener.onDisableFailed(new ModuleDisableEvent.Failed(context, new IllegalStateException("disable failed"))));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> registry.getStorage(context));
        assertExceptionMessageContains(exception, "BlueStorage should be registered for persistence-enabled modules");
        assertExceptionMessageContains(exception, "unexpected persistence extension lifecycle state");
        assertExceptionMessageContains(exception, "plain");
    }

    @Test
    void nonSourceProviderCannotInjectBlueStorage() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(ConstructorPersistenceModule.class);

        listener.onContainerCreated(new BlueMatrixContainerEvent.Created(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));

        assertFalse(registry.containsStorage(candidate));
        ModuleInstantiationException exception = assertThrows(ModuleInstantiationException.class, () -> new DefaultModuleInstanceFactory(parameterResolvers)
                .create(candidate));
        assertCauseMessageContains(exception, "BlueStorage injection requires module to implement BlueStorageSourceProvider");
        assertCauseMessageContains(exception, "constructor-persistence");
        assertCauseMessageContains(exception, ConstructorPersistenceModule.class.getName());
    }

    @Test
    void moduleWithSourceProviderInitializesInjectedStorage() {
        TestModulePersistenceRegistry registry = new TestModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        StorageProviderModule module = createModule(listener, StorageProviderModule.class);
        ModuleContext context = context(module);
        RecordingBlueStorage storage = (RecordingBlueStorage) registry.getStorage(context);

        assertFalse(module.storage.available());

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertTrue(module.storage.available());
        assertNotNull(storage.initializedStorage);
        assertSame(storage.initializedStorage, module.storage.storage());
        assertSame(module.storage, registry.getStorage(context));
        assertEquals(1, storage.initializeCalls);
        assertTrue(storage.repositoryTypes.contains(AutoRegisteredEntity.class));
    }

    @Test
    void sourceProviderCanInjectBlueStorageField() {
        TestModulePersistenceRegistry registry = new TestModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        FieldPersistenceModule module = createModule(listener, FieldPersistenceModule.class);
        ModuleContext context = context(module);
        RecordingBlueStorage storage = (RecordingBlueStorage) registry.getStorage(context);

        assertFalse(module.storage.available());

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));

        assertNotNull(storage.initializedStorage);
        assertSame(storage.initializedStorage, module.storage.storage());
        assertSame(module.storage, registry.getStorage(context));
    }

    @Test
    void sourceProviderFailureReportsEnablePreError() {
        RuntimeException failure = new RuntimeException("storage source failed");
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        ThrowingSourceProviderModule module = new ThrowingSourceProviderModule(failure);
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(ThrowingSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("persistence", event.getErrorSource());
        assertEquals("Module persistence initialization failed", event.getErrorMessage());
        assertSame(failure, event.getErrorCause());
    }

    @Test
    void nullSourceReportsEnablePreError() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        NullSourceProviderModule module = new NullSourceProviderModule();
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(NullSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("persistence", event.getErrorSource());
        assertEquals("Module persistence initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
    }

    @Test
    void failingSourceConversionReportsEnablePreError() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        InvalidSqlSourceProviderModule module = new InvalidSqlSourceProviderModule();
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(InvalidSqlSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("persistence", event.getErrorSource());
        assertEquals("Module persistence initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
    }

    @Test
    void invalidLocalFileSourceReportsEnablePreError() {
        ModulePersistenceRegistry registry = new ModulePersistenceRegistry(DATA_FOLDER);
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        InvalidLocalFileSourceProviderModule module = new InvalidLocalFileSourceProviderModule("../outside");
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate(InvalidLocalFileSourceProviderModule.class)));
        ModuleEnableEvent.Pre event = new ModuleEnableEvent.Pre(context(module));

        assertDoesNotThrow(() -> listener.onEnablePre(event));

        assertTrue(event.hasError());
        assertEquals("persistence", event.getErrorSource());
        assertEquals("Module persistence initialization failed", event.getErrorMessage());
        assertTrue(event.getErrorCause() instanceof IllegalArgumentException);
    }

    @Test
    void sqlSourceConvertsToSqlSpec() {
        BlueStorageSpec spec = new TestSqlSource(BlueSqlType.SQL).toSpec(sourceContext());

        SqlConfig config = assertInstanceOf(SqlConfig.class, spec.config());
        assertEquals("jdbc:h2:mem:test", config.jdbcUrl());
        assertEquals("", config.username());
        assertEquals("", config.password());
        PoolTuning pool = config.pool();
        assertEquals(1, pool.minIdle());
        assertEquals(3, pool.maxSize());
        assertEquals(5000L, pool.connectTimeout().toMillis());
        assertEquals(60000L, pool.idleTimeout().toMillis());
        assertEquals(120000L, pool.maxLifetime().toMillis());
    }

    @Test
    void sqlSourceConvertsToPostgresqlSpec() {
        BlueStorageSpec spec = new TestSqlSource(BlueSqlType.POSTGRESQL).toSpec(sourceContext());

        assertInstanceOf(SqlConfig.class, spec.config());
    }

    @Test
    void sqlSourceConvertsToH2Spec() {
        BlueStorageSpec spec = new TestSqlSource(BlueSqlType.H2).toSpec(sourceContext());

        assertInstanceOf(SqlConfig.class, spec.config());
    }

    @Test
    void mongoSourceConvertsToSpec() {
        BlueStorageSpec spec = new TestMongoSource().toSpec(sourceContext());

        MongoConfig config = assertInstanceOf(MongoConfig.class, spec.config());
        assertEquals("mongodb://localhost:27017", config.connectionString());
        assertEquals("test", config.database());
        assertTrue(config.connectTimeout().isPresent());
        assertEquals(10000L, config.connectTimeout().get().toMillis());
    }

    @Test
    void localFileSourceConvertsToSpec() {
        BlueStorageSourceContext context = sourceContext();
        BlueStorageSpec spec = new TestLocalFileSource().toSpec(context);

        LocalFileConfig config = assertInstanceOf(LocalFileConfig.class, spec.config());
        assertEquals(new File(context.storageRootDirectory(), "storage").toPath(), config.baseDirectory());
        assertFalse(config.prettyPrint());
        assertTrue(config.fsyncEvery().isPresent());
        assertEquals(30000L, config.fsyncEvery().get().toMillis());
    }

    @Test
    void localFileSourceDefaultsToModuleDataDirectory() {
        BlueStorageSourceContext context = sourceContext();
        BlueStorageSpec spec = localFileSource(" ").toSpec(context);

        LocalFileConfig config = assertInstanceOf(LocalFileConfig.class, spec.config());
        assertEquals(context.storageRootDirectory().toPath(), config.baseDirectory());
    }

    @Test
    void localFileSourceRejectsUnsafeBaseDirectory() {
        assertThrows(IllegalArgumentException.class, () -> localFileSource("/tmp/outside").toSpec(sourceContext()));
        assertThrows(IllegalArgumentException.class, () -> localFileSource("C:\\outside").toSpec(sourceContext()));
        assertThrows(IllegalArgumentException.class, () -> localFileSource("data/../outside").toSpec(sourceContext()));
    }

    @Test
    void inMemorySourceConvertsToSpec() {
        BlueStorageSpec spec = new TestInMemorySource().toSpec(sourceContext());

        assertInstanceOf(InMemoryConfig.class, spec.config());
    }

    @Test
    void disableClosesInitializedStorage() {
        TestModulePersistenceRegistry registry = new TestModulePersistenceRegistry();
        PersistenceModuleListener listener = new PersistenceModuleListener(registry);
        StorageProviderModule module = createModule(listener, StorageProviderModule.class);
        ModuleContext context = context(module);
        RecordingBlueStorage storage = (RecordingBlueStorage) registry.getStorage(context);

        listener.onEnablePre(new ModuleEnableEvent.Pre(context));
        listener.onDisablePost(new ModuleDisableEvent.Post(context));
        listener.onDisableFailed(new ModuleDisableEvent.Failed(context, new IllegalStateException("disable failed")));

        assertEquals(2, storage.closeCalls);
    }

    private static ModuleCandidate candidate(Class<? extends Module> type) {
        return new ModuleCandidate(type, type.getAnnotation(ModuleInfo.class));
    }

    private static ModuleContext context(Module module) {
        return new ModuleContext(module, module.getClass().getAnnotation(ModuleInfo.class));
    }

    private static BlueStorageSourceContext sourceContext() {
        PlainModule module = new PlainModule();
        String moduleId = module.getClass().getAnnotation(ModuleInfo.class).id();
        return new BlueStorageSourceContext(
                new ModulePersistenceRegistry(DATA_FOLDER).getModuleDataPath(moduleId)
        );
    }

    private static BlueLocalFileStorageSource localFileSource(final String baseDirectory) {
        return new TestLocalFileSource() {
            @Override
            public String getBaseDirectory() {
                return baseDirectory;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends Module> T createModule(PersistenceModuleListener listener, Class<T> type) {
        ModuleParameterResolverRegistry parameterResolvers = new ModuleParameterResolverRegistry();
        ModuleCandidate candidate = candidate(type);
        listener.onContainerCreated(new BlueMatrixContainerEvent.Created(parameterResolvers));
        listener.onRegisterPre(new ModuleRegisterEvent.Pre(candidate));
        return (T) new DefaultModuleInstanceFactory(parameterResolvers).create(candidate);
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

    @ModuleInfo(id = "constructor-persistence", name = "Constructor Persistence")
    private static class ConstructorPersistenceModule implements Module {
        @SuppressWarnings("unused")
        private final BlueStorage storage;

        private ConstructorPersistenceModule(BlueStorage storage) {
            this.storage = storage;
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

    @ModuleInfo(id = "storage-provider", name = "Storage Provider")
    private static class StorageProviderModule implements Module, BlueStorageSourceProvider {
        private final BlueStorage storage;
        private final BlueStorageSource source = new TestInMemorySource();

        private StorageProviderModule(BlueStorage storage) {
            this.storage = storage;
        }

        @Override
        public BlueStorageSource getStorageSource() {
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

    @ModuleInfo(id = "field-persistence", name = "Field Persistence")
    private static class FieldPersistenceModule implements Module, BlueStorageSourceProvider {
        @ModuleInject
        private BlueStorage storage;
        private final BlueStorageSource source = new TestInMemorySource();

        @Override
        public BlueStorageSource getStorageSource() {
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
    private static class ThrowingSourceProviderModule implements Module, BlueStorageSourceProvider {
        private final RuntimeException failure;

        private ThrowingSourceProviderModule(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public BlueStorageSource getStorageSource() {
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
    private static class NullSourceProviderModule implements Module, BlueStorageSourceProvider {
        @Override
        public BlueStorageSource getStorageSource() {
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

    @ModuleInfo(id = "invalid-sql-source-provider", name = "Invalid SQL Source Provider")
    private static class InvalidSqlSourceProviderModule implements Module, BlueStorageSourceProvider {
        @Override
        public BlueStorageSource getStorageSource() {
            return new TestSqlSource(null);
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

    @ModuleInfo(id = "invalid-local-source-provider", name = "Invalid Local Source Provider")
    private static class InvalidLocalFileSourceProviderModule implements Module, BlueStorageSourceProvider {
        private final String baseDirectory;

        private InvalidLocalFileSourceProviderModule(String baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        @Override
        public BlueStorageSource getStorageSource() {
            return localFileSource(baseDirectory);
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

    private static class TestSqlSource implements BlueSqlStorageSource {
        private final BlueSqlType sqlType;

        private TestSqlSource(BlueSqlType sqlType) {
            this.sqlType = sqlType;
        }

        @Override
        public BlueSqlType getSqlType() {
            return sqlType;
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:h2:mem:test";
        }

        @Override
        public String getUsername() {
            return "";
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public int getPoolMinIdle() {
            return 1;
        }

        @Override
        public int getPoolMaxSize() {
            return 3;
        }

        @Override
        public long getPoolConnectTimeoutMillis() {
            return 5000L;
        }

        @Override
        public long getPoolIdleTimeoutMillis() {
            return 60000L;
        }

        @Override
        public long getPoolMaxLifetimeMillis() {
            return 120000L;
        }
    }

    private static class TestMongoSource implements BlueMongoStorageSource {
        @Override
        public String getConnectionString() {
            return "mongodb://localhost:27017";
        }

        @Override
        public String getDatabase() {
            return "test";
        }

        @Override
        public long getConnectTimeoutMillis() {
            return 10000L;
        }
    }

    private static class TestLocalFileSource implements BlueLocalFileStorageSource {
        @Override
        public String getBaseDirectory() {
            return "storage";
        }

        @Override
        public boolean isPrettyPrint() {
            return false;
        }

        @Override
        public long getFsyncEveryMillis() {
            return 30000L;
        }
    }

    private static class TestInMemorySource implements BlueInMemoryStorageSource {
    }

    private static class TestModulePersistenceRegistry extends ModulePersistenceRegistry {
        private TestModulePersistenceRegistry() {
            super(DATA_FOLDER);
        }

        @Override
        protected BlueStorage createStorage() {
            return new RecordingBlueStorage();
        }
    }

    private static class RecordingBlueStorage extends BlueStorage {
        private int initializeCalls;
        private int closeCalls;
        private Storage initializedStorage;
        private final java.util.List<Class<?>> repositoryTypes = new java.util.ArrayList<Class<?>>();

        @Override
        public synchronized void initialize(Storage storage) {
            initializeCalls++;
            initializedStorage = storage;
            super.initialize(storage);
        }

        @Override
        public <K, V> Repository<K, V> repository(Class<V> entityType) {
            repositoryTypes.add(entityType);
            return super.repository(entityType);
        }

        @Override
        public void close() {
            closeCalls++;
            super.close();
        }
    }

    @BlueEntity(collection = "auto_registered_entities")
    private static final class AutoRegisteredEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();
    }
}
