package io.fntlv.bluematrix.persistence.core;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    void classBasedDescriptorUsesInternalDescriptorCache() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());

        assertDoesNotThrow(() -> storage.descriptor(StorageEntity.class));
    }

    @Test
    void classBasedDescriptorIsCachedByStorage() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());

        assertSame(storage.descriptor(StorageEntity.class), storage.descriptor(StorageEntity.class));
    }

    @Test
    void classBasedRepositoryUsesInternalDescriptorCache() {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());

        assertDoesNotThrow(() -> storage.repository(StorageEntity.class));
    }

    @BlueEntity(collection = "storage_entities")
    private static final class StorageEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();
    }
}
