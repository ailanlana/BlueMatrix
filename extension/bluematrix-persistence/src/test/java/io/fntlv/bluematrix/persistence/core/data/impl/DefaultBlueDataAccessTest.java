package io.fntlv.bluematrix.persistence.core.data.impl;

import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;
import br.com.finalcraft.everydatabase.query.Query;
import br.com.finalcraft.everydatabase.query.QueryOptions;
import io.fntlv.bluematrix.persistence.core.cache.BlueCache;
import io.fntlv.bluematrix.persistence.core.cache.BlueCachePolicy;
import io.fntlv.bluematrix.persistence.core.data.BlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.BlueDataQueryAccess;
import io.fntlv.bluematrix.persistence.core.data.DefaultBlueDataAccess;
import io.fntlv.bluematrix.persistence.core.data.definition.CachedBlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinitionFactory;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultBlueDataAccessTest {

    @Test
    void directDefinitionSupportsBasicCrud() {
        BlueStorage storage = storage(DirectData.class);
        BlueDataAccess access = new DefaultBlueDataAccess(storage);
        DirectData data = new DirectData(DirectData.FIRST_ID, "first");

        access.save(data).join();
        Optional<DirectData> found = access.get(DirectData.class, DirectData.FIRST_ID).join();
        List<DirectData> all = access.getAll(DirectData.class, Arrays.asList(DirectData.FIRST_ID, DirectData.SECOND_ID)).join();
        boolean deleted = access.delete(DirectData.class, DirectData.FIRST_ID).join();
        Optional<DirectData> missing = access.get(DirectData.class, DirectData.FIRST_ID).join();

        assertTrue(found.isPresent());
        assertEquals("first", found.get().name);
        assertEquals(1, all.size());
        assertTrue(deleted);
        assertFalse(missing.isPresent());
    }

    @Test
    void cachedDefinitionSupportsBasicCrud() {
        BlueStorage storage = storage(CachedData.class);
        BlueDataAccess access = new DefaultBlueDataAccess(storage);
        CachedData data = new CachedData(CachedData.FIRST_ID, "first");

        access.save(data).join();
        Optional<CachedData> found = access.get(CachedData.class, CachedData.FIRST_ID).join();
        List<CachedData> all = access.getAll(CachedData.class, Arrays.asList(CachedData.FIRST_ID, CachedData.SECOND_ID)).join();
        boolean deleted = access.delete(CachedData.class, CachedData.FIRST_ID).join();
        Optional<CachedData> missing = access.get(CachedData.class, CachedData.FIRST_ID).join();

        assertTrue(found.isPresent());
        assertEquals("first", found.get().name);
        assertEquals(1, all.size());
        assertTrue(deleted);
        assertFalse(missing.isPresent());
    }

    @Test
    void queryAndCountUseRegisteredDefinition() {
        BlueStorage storage = storage(DirectData.class);
        DefaultBlueDataAccess access = new DefaultBlueDataAccess(storage);
        BlueDataAccess dataAccess = access;
        BlueDataQueryAccess queryAccess = access;
        dataAccess.save(new DirectData(DirectData.FIRST_ID, "first")).join();
        dataAccess.save(new DirectData(DirectData.SECOND_ID, "second")).join();

        List<DirectData> rows = queryAccess.query(DirectData.class, Query.all()).join();
        List<DirectData> rowsWithOptions = queryAccess.query(DirectData.class, Query.all(), QueryOptions.none()).join();
        long count = queryAccess.count(DirectData.class, Query.all()).join();

        assertEquals(2, rows.size());
        assertEquals(2, rowsWithOptions.size());
        assertEquals(2L, count);
    }

    @Test
    void cachedQueryDoesNotSeedCache() {
        BlueStorage storage = storage(CachedData.class);
        BlueDataQueryAccess access = new DefaultBlueDataAccess(storage);
        CachedBlueDataDefinition<UUID, CachedData> definition = cachedDefinition(storage, CachedData.class);
        definition.manager().repository().save(new CachedData(CachedData.FIRST_ID, "first")).join();

        List<CachedData> rows = access.query(CachedData.class, Query.all()).join();

        assertEquals(1, rows.size());
        assertEquals(0, definition.manager().cachedSize());
    }

    @Test
    void refRequiresCachedDefinition() {
        BlueStorage storage = storage(CachedData.class, DirectData.class);
        BlueDataAccess access = new DefaultBlueDataAccess(storage);

        Ref<UUID, CachedData> ref = access.ref(CachedData.FIRST_ID, CachedData.class);
        Ref<UUID, CachedData> policyRef = access.ref(CachedData.FIRST_ID, CachedData.class, CachePolicy.always());

        assertNotNull(ref);
        assertNotNull(policyRef);
        assertThrows(IllegalArgumentException.class, () -> access.ref(DirectData.FIRST_ID, DirectData.class));
    }

    @Test
    void flushDirtyUsesRegisteredDefinition() {
        BlueStorage storage = storage(CachedData.class, DirectData.class);
        BlueDataAccess access = new DefaultBlueDataAccess(storage);

        assertTrue(access.flushDirty(CachedData.class).join().isEmpty());
        assertTrue(access.flushDirty(DirectData.class).join().isEmpty());
    }

    @Test
    void flushAllDirtyFlushesAllDefinitions() {
        BlueStorage storage = storage(CachedData.class, DirectData.class);
        BlueDataAccess access = new DefaultBlueDataAccess(storage);

        List<BatchSaveReport<?>> reports = access.flushAllDirty().join();

        assertEquals(2, reports.size());
        assertTrue(reports.get(0).isEmpty());
        assertTrue(reports.get(1).isEmpty());
    }

    @Test
    void missingDefinitionFails() {
        BlueDataAccess access = new DefaultBlueDataAccess(storage());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> access.get(DirectData.class, DirectData.FIRST_ID));

        assertTrue(exception.getMessage().contains("not registered"));
    }

    @Test
    void rejectsNullArguments() {
        BlueDataAccess access = new DefaultBlueDataAccess(storage(DirectData.class));
        BlueDataQueryAccess queryAccess = new DefaultBlueDataAccess(storage(DirectData.class));

        assertThrows(IllegalArgumentException.class, () -> new DefaultBlueDataAccess(null));
        assertThrows(IllegalArgumentException.class, () -> access.get(null, DirectData.FIRST_ID));
        assertThrows(IllegalArgumentException.class, () -> access.get(DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> access.getAll(DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> access.save(null));
        assertThrows(IllegalArgumentException.class, () -> access.delete(DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> access.ref(null, DirectData.class));
        assertThrows(IllegalArgumentException.class, () -> access.ref(DirectData.FIRST_ID, DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> access.flushDirty(null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.query(DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.query(DirectData.class, Query.all(), null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.count(DirectData.class, null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.querySlice(DirectData.class, null, QueryOptions.none()));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.querySlice(DirectData.class, Query.all(), null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.queryPage(DirectData.class, null, QueryOptions.none()));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.queryPage(DirectData.class, Query.all(), null));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.queryAfter(DirectData.class, null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> queryAccess.queryAfter(DirectData.class, Query.all(), null, 10));
    }

    @SuppressWarnings("unchecked")
    private static <V> CachedBlueDataDefinition<UUID, V> cachedDefinition(BlueStorage storage, Class<V> type) {
        return (CachedBlueDataDefinition<UUID, V>) storage.registry().find(type).get();
    }

    private static BlueStorage storage(Class<?>... dataTypes) {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();
        for (Class<?> dataType : dataTypes) {
            register(storage, factory, dataType);
        }
        return storage;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(BlueStorage storage, BlueDataDefinitionFactory factory, Class dataType) {
        storage.registry().register(factory.create(dataType, storage));
    }

    @BlueEntity(collection = "direct_access_data")
    private static final class DirectData {
        private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
        private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000041");

        @BlueKey
        public UUID id;
        public String name;

        public DirectData() {
        }

        private DirectData(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @BlueEntity(collection = "cached_access_data")
    @BlueCache(policy = BlueCachePolicy.ALWAYS)
    private static final class CachedData {
        private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
        private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000043");

        @BlueKey
        public UUID id;
        public String name;

        public CachedData() {
        }

        private CachedData(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
