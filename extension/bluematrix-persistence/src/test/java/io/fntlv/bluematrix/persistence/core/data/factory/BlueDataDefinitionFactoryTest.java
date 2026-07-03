package io.fntlv.bluematrix.persistence.core.data.factory;

import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import br.com.finalcraft.everydatabase.manager.cache.CacheEntry;
import io.fntlv.bluematrix.persistence.core.cache.BlueCache;
import io.fntlv.bluematrix.persistence.core.cache.BlueCachePolicy;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinitionFactory;
import io.fntlv.bluematrix.persistence.core.data.definition.CachedBlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.DirectBlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueDataDefinitionFactoryTest {

    @Test
    void createUsesDirectDefinitionWhenBlueCacheIsMissing() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();
        BlueStorage storage = storage();
        BlueDataDefinition<UUID, DirectData> definition = factory.create(DirectData.class, storage);
        DirectData data = new DirectData(DirectData.ID);

        DirectBlueDataDefinition<UUID, DirectData> direct = (DirectBlueDataDefinition<UUID, DirectData>) definition;
        direct.repository().save(data).join();
        Optional<DirectData> found = direct.repository().find(DirectData.ID).join();

        assertTrue(definition instanceof DirectBlueDataDefinition);
        assertEquals(DirectData.class, definition.type());
        assertEquals(UUID.class, definition.keyType());
        assertEquals("direct_data", definition.collection());
        assertEquals(DirectData.ID, definition.key(data));
        assertTrue(direct.descriptor().codec() instanceof JacksonJsonCodec);
        assertTrue(found.isPresent());
        assertEquals(DirectData.ID, found.get().id);
    }

    @Test
    void createUsesCachedDefinitionWhenBlueCacheIsPresent() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();
        BlueStorage storage = storage();
        BlueDataDefinition<UUID, CachedData> definition = factory.create(CachedData.class, storage);
        CachedData data = new CachedData(CachedData.ID);

        CachedBlueDataDefinition<UUID, CachedData> cached = (CachedBlueDataDefinition<UUID, CachedData>) definition;
        cached.manager().saveAndCache(data).join();
        CacheEntry<CachedData> cell = cached.manager()
                .resolveCell(CachedData.ID, cached.manager().defaultPolicy())
                .join();

        assertTrue(definition instanceof CachedBlueDataDefinition);
        assertEquals(CachedData.class, definition.type());
        assertEquals(UUID.class, definition.keyType());
        assertEquals("cached_data", definition.collection());
        assertEquals(CachedData.ID, definition.key(data));
        assertTrue(storage.refRegistry().isRegistered(CachedData.class));
        assertNotNull(cell);
        assertEquals(CachedData.ID, cell.getValue().id);
    }

    @Test
    void cachedDefinitionUsesBlueCacheOptions() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();
        BlueStorage storage = storage();
        BlueDataDefinition<UUID, SmallCachedData> definition = factory.create(SmallCachedData.class, storage);
        CachedBlueDataDefinition<UUID, SmallCachedData> cached =
                (CachedBlueDataDefinition<UUID, SmallCachedData>) definition;

        cached.manager().saveAndCache(new SmallCachedData(SmallCachedData.FIRST_ID)).join();
        cached.manager().saveAndCache(new SmallCachedData(SmallCachedData.SECOND_ID)).join();

        assertTrue(cached.manager().cachedSize() <= 1);
    }

    @Test
    void createUsesCachedDefinitionForTtlBlueCache() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();

        BlueDataDefinition<UUID, TtlCachedData> definition = factory.create(TtlCachedData.class, storage());

        assertTrue(definition instanceof CachedBlueDataDefinition);
    }

    @Test
    void createUsesDirectDefinitionForNoCacheBlueCache() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();

        BlueDataDefinition<UUID, NoCacheData> definition = factory.create(NoCacheData.class, storage());

        assertTrue(definition instanceof DirectBlueDataDefinition);
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new BlueDataDefinitionFactory(null));
    }

    @Test
    void createRejectsNullArguments() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();

        assertThrows(IllegalArgumentException.class, () -> factory.create(null, storage()));
        assertThrows(IllegalArgumentException.class, () -> factory.create(DirectData.class, null));
    }

    @Test
    void createRequiresInitializedStorage() {
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();

        assertThrows(IllegalStateException.class, () -> factory.create(DirectData.class, new BlueStorage()));
        assertThrows(IllegalStateException.class, () -> factory.create(CachedData.class, new BlueStorage()));
    }

    private static BlueStorage storage() {
        return new BlueStorage(Storages.createInMemory());
    }

    @BlueEntity(collection = "direct_data")
    private static final class DirectData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

        @BlueKey
        public UUID id;

        private DirectData() {
        }

        private DirectData(UUID id) {
            this.id = id;
        }
    }

    @BlueEntity(collection = "cached_data")
    @BlueCache(policy = BlueCachePolicy.ALWAYS)
    private static final class CachedData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000031");

        @BlueKey
        public UUID id;

        private CachedData() {
        }

        private CachedData(UUID id) {
            this.id = id;
        }
    }

    @BlueEntity(collection = "ttl_cached_data")
    @BlueCache(policy = BlueCachePolicy.TTL, ttlSeconds = 5)
    private static final class TtlCachedData {
        @BlueKey
        public UUID id;
    }

    @BlueEntity(collection = "no_cache_data")
    @BlueCache(policy = BlueCachePolicy.NO_CACHE)
    private static final class NoCacheData {
        @BlueKey
        public UUID id;
    }

    @BlueEntity(collection = "small_cached_data")
    @BlueCache(maxSize = 1, policy = BlueCachePolicy.ALWAYS)
    private static final class SmallCachedData {
        private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000032");
        private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000033");

        @BlueKey
        public UUID id;

        private SmallCachedData() {
        }

        private SmallCachedData(UUID id) {
            this.id = id;
        }
    }
}
