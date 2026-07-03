package io.fntlv.bluematrix.persistence.core.data.definition;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.Storage;
import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import br.com.finalcraft.everydatabase.manager.CachingManager;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.RefRegistry;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;
import br.com.finalcraft.everydatabase.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueDataDefinitionTest {

    @Test
    void directDefinitionExposesDataMetadata() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        Repository<UUID, SampleData> repository = storage().repository(descriptor);
        DirectBlueDataDefinition<UUID, SampleData> definition =
                new DirectBlueDataDefinition<UUID, SampleData>(descriptor, repository);
        SampleData data = new SampleData(SampleData.ID);

        assertEquals(SampleData.class, definition.type());
        assertEquals(UUID.class, definition.keyType());
        assertEquals("sample_data", definition.collection());
        assertEquals(SampleData.ID, definition.key(data));
        assertSame(descriptor, definition.descriptor());
        assertSame(repository, definition.repository());
        assertThrows(IllegalArgumentException.class, () -> definition.ref(SampleData.ID));
    }

    @Test
    void cachedDefinitionExposesDataMetadata() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        RefRegistry refRegistry = new RefRegistry();
        CachingManager<UUID, SampleData> manager = refRegistry.manager(descriptor, storage(), CachePolicy.always());
        CachedBlueDataDefinition<UUID, SampleData> definition =
                new CachedBlueDataDefinition<UUID, SampleData>(descriptor, manager, refRegistry);
        SampleData data = new SampleData(SampleData.ID);

        assertEquals(SampleData.class, definition.type());
        assertEquals(UUID.class, definition.keyType());
        assertEquals("sample_data", definition.collection());
        assertEquals(SampleData.ID, definition.key(data));
        assertSame(descriptor, definition.descriptor());
        assertSame(manager, definition.manager());
        assertSame(manager.repository(), definition.repository());
        Ref<UUID, SampleData> ref = definition.ref(SampleData.ID);
        assertNotNull(ref);
    }

    @Test
    void directDefinitionRepositoryCanSaveAndFindData() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        Repository<UUID, SampleData> repository = storage().repository(descriptor);
        DirectBlueDataDefinition<UUID, SampleData> definition =
                new DirectBlueDataDefinition<UUID, SampleData>(descriptor, repository);
        SampleData data = new SampleData(SampleData.ID);

        definition.save(data).join();
        Optional<SampleData> found = definition.get(SampleData.ID).join();
        List<SampleData> all = definition.getAll(Arrays.asList(SampleData.ID)).join();
        boolean deleted = definition.delete(SampleData.ID).join();

        assertTrue(found.isPresent());
        assertEquals(SampleData.ID, found.get().id);
        assertEquals(1, all.size());
        assertTrue(deleted);
    }

    @Test
    void directDefinitionFlushDirtyReturnsEmptyReport() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        DirectBlueDataDefinition<UUID, SampleData> definition =
                new DirectBlueDataDefinition<UUID, SampleData>(descriptor, storage().repository(descriptor));

        assertTrue(definition.flushDirty().join().isEmpty());
    }

    @Test
    void cachedDefinitionManagerCanSaveAndResolveData() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        RefRegistry refRegistry = new RefRegistry();
        CachingManager<UUID, SampleData> manager = refRegistry.manager(descriptor, storage(), CachePolicy.always());
        CachedBlueDataDefinition<UUID, SampleData> definition =
                new CachedBlueDataDefinition<UUID, SampleData>(descriptor, manager, refRegistry);
        SampleData data = new SampleData(SampleData.ID);

        definition.save(data).join();
        Optional<SampleData> found = definition.get(SampleData.ID).join();
        List<SampleData> all = definition.getAll(Arrays.asList(SampleData.ID)).join();
        boolean deleted = definition.delete(SampleData.ID).join();
        Optional<SampleData> afterDelete = definition.get(SampleData.ID).join();

        assertTrue(refRegistry.isRegistered(SampleData.class));
        assertTrue(found.isPresent());
        assertEquals(1, all.size());
        assertTrue(deleted);
        assertFalse(afterDelete.isPresent());
    }

    @Test
    void cachedDefinitionFlushDirtyDelegatesToManager() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        RefRegistry refRegistry = new RefRegistry();
        CachingManager<UUID, SampleData> manager = refRegistry.manager(descriptor, storage(), CachePolicy.always());
        CachedBlueDataDefinition<UUID, SampleData> definition =
                new CachedBlueDataDefinition<UUID, SampleData>(descriptor, manager, refRegistry);

        assertTrue(definition.flushDirty().join().isEmpty());
    }

    @Test
    void directDefinitionExposesRepositoryForQueries() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        DirectBlueDataDefinition<UUID, SampleData> definition =
                new DirectBlueDataDefinition<UUID, SampleData>(descriptor, storage().repository(descriptor));

        definition.save(new SampleData(SampleData.ID)).join();
        List<SampleData> rows = definition.repository().query(Query.all()).join();
        long count = definition.repository().count(Query.all()).join();

        assertEquals(1, rows.size());
        assertEquals(1L, count);
    }

    @Test
    void cachedDefinitionRepositoryQueriesDoNotSeedCache() {
        EntityDescriptor<UUID, SampleData> descriptor = descriptor();
        RefRegistry refRegistry = new RefRegistry();
        CachingManager<UUID, SampleData> manager = refRegistry.manager(descriptor, storage(), CachePolicy.always());
        CachedBlueDataDefinition<UUID, SampleData> definition =
                new CachedBlueDataDefinition<UUID, SampleData>(descriptor, manager, refRegistry);
        manager.repository().save(new SampleData(SampleData.ID)).join();

        List<SampleData> rows = definition.repository().query(Query.all()).join();

        assertEquals(1, rows.size());
        assertEquals(0, manager.cachedSize());
    }

    private static Storage storage() {
        Storage storage = Storages.createInMemory();
        storage.init().join();
        return storage;
    }

    private static EntityDescriptor<UUID, SampleData> descriptor() {
        return EntityDescriptor.builder(UUID.class, SampleData.class)
                .collection("sample_data")
                .keyExtractor(data -> data.id)
                .codec(new JacksonJsonCodec<SampleData>(SampleData.class))
                .build();
    }

    private static final class SampleData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

        public UUID id;

        public SampleData() {
        }

        private SampleData(UUID id) {
            this.id = id;
        }
    }
}
