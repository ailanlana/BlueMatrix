package io.fntlv.bluematrix.persistence.core.storage;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueStorageTest {

    @Test
    void repositoryWithExplicitDescriptorDoesNotNeedDescriptorCache() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());
        EntityDescriptor<UUID, StorageEntity> descriptor = EntityDescriptor.builder(UUID.class, StorageEntity.class)
                .collection("storage_entities")
                .keyExtractor(entity -> entity.id)
                .codec(new JacksonJsonCodec<StorageEntity>(StorageEntity.class))
                .build();

        assertDoesNotThrow(() -> storage.repository(descriptor));
    }

    @Test
    void exposesStableDataComponents() {
        BlueStorage storage = new BlueStorage();

        assertNotNull(storage.registry());
        assertNotNull(storage.refRegistry());
        assertSame(storage.registry(), storage.registry());
        assertSame(storage.refRegistry(), storage.refRegistry());
    }

    @Test
    void storageIsUnavailableBeforeInitialize() {
        BlueStorage storage = new BlueStorage();

        assertThrows(IllegalStateException.class, storage::storage);
    }

    @Test
    void initializeRejectsNullStorage() {
        BlueStorage storage = new BlueStorage();

        assertThrows(IllegalArgumentException.class, () -> storage.initialize(null));
    }

    @Test
    void initializeRejectsSecondStorage() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());

        assertThrows(IllegalStateException.class, () -> storage.initialize(Storages.createInMemory()));
    }

    @Test
    void constructorInitializesStorage() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());

        assertTrue(storage.available());
        assertNotNull(storage.storage());
    }

    private static final class StorageEntity {
        private UUID id = UUID.randomUUID();
    }
}
