package io.fntlv.bluematrix.persistence.core.descriptor;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.codec.Codec;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import br.com.finalcraft.everydatabase.query.IndexHint;
import br.com.finalcraft.everydatabase.query.Indexed;
import br.com.finalcraft.everydatabase.versioned.OptimisticLock;
import br.com.finalcraft.everydatabase.versioned.Versioned;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueEntityDescriptorFactoryTest {

    @Test
    void fieldKeyCreatesDescriptor() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, FieldKeyEntity> descriptor = factory.create(FieldKeyEntity.class);

        assertEquals("field_entities", descriptor.collection());
        assertEquals(UUID.class, descriptor.keyType());
        assertEquals(FieldKeyEntity.class, descriptor.type());
        assertEquals(FieldKeyEntity.ID, descriptor.keyExtractor().apply(new FieldKeyEntity()));
        assertTrue(descriptor.codec() instanceof JacksonJsonCodec);
    }

    @Test
    void methodKeyCreatesDescriptor() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<String, MethodKeyEntity> descriptor = factory.create(MethodKeyEntity.class);

        assertEquals("method_entities", descriptor.collection());
        assertEquals(String.class, descriptor.keyType());
        assertEquals("method-key", descriptor.keyExtractor().apply(new MethodKeyEntity()));
    }

    @Test
    void descriptorIsCreatedEachTime() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, FieldKeyEntity> first = factory.create(FieldKeyEntity.class);
        EntityDescriptor<UUID, FieldKeyEntity> second = factory.create(FieldKeyEntity.class);

        assertNotSame(first, second);
    }

    @Test
    void customCodecFactoryIsUsed() {
        CustomCodecFactory.calls = 0;
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, CustomCodecEntity> descriptor = factory.create(CustomCodecEntity.class);

        assertEquals(1, CustomCodecFactory.calls);
        assertEquals("application/custom", descriptor.codec().contentType());
    }

    @Test
    void indexedAnnotationIsPreservedByEveryDatabaseBuilder() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, IndexedEntity> descriptor = factory.create(IndexedEntity.class);

        assertEquals(1, descriptor.indexes().size());
        IndexHint index = descriptor.indexes().get(0);
        assertEquals("name", index.fieldPath());
        assertEquals(IndexHint.FieldType.STRING, index.fieldType());
    }

    @Test
    void blueIndexCreatesIndex() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, BlueIndexedEntity> descriptor = factory.create(BlueIndexedEntity.class);

        assertEquals(1, descriptor.indexes().size());
        IndexHint index = descriptor.indexes().get(0);
        assertEquals("name", index.fieldPath());
        assertEquals(IndexHint.FieldType.STRING, index.fieldType());
        assertEquals(IndexHint.Order.ASCENDING, index.order());
    }

    @Test
    void blueIndexSupportsDescendingOrder() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, BlueDescendingIndexedEntity> descriptor = factory.create(BlueDescendingIndexedEntity.class);

        assertEquals(1, descriptor.indexes().size());
        IndexHint index = descriptor.indexes().get(0);
        assertEquals("score", index.fieldPath());
        assertEquals(IndexHint.FieldType.INT, index.fieldType());
        assertEquals(IndexHint.Order.DESCENDING, index.order());
    }

    @Test
    void blueIndexSupportsNestedPathAndExplicitType() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, BlueNestedIndexedEntity> descriptor = factory.create(BlueNestedIndexedEntity.class);

        assertEquals(1, descriptor.indexes().size());
        IndexHint index = descriptor.indexes().get(0);
        assertEquals("guild.id", index.fieldPath());
        assertEquals(IndexHint.FieldType.STRING, index.fieldType());
    }

    @Test
    void optimisticLockAnnotationIsPreservedByEveryDatabaseBuilder() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, OptimisticLockEntity> descriptor = factory.create(OptimisticLockEntity.class);

        assertTrue(descriptor.isVersioned());
    }

    @Test
    void blueOptimisticLockEnablesVersionedDescriptor() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, BlueOptimisticLockEntity> descriptor = factory.create(BlueOptimisticLockEntity.class);

        assertTrue(descriptor.isVersioned());
    }

    @Test
    void versionedFlagCallsEveryDatabaseVersionedBuilder() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        EntityDescriptor<UUID, VersionedEntity> descriptor = factory.create(VersionedEntity.class);

        assertTrue(descriptor.isVersioned());
    }

    @Test
    void missingBlueEntityFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(MissingEntityAnnotation.class));

        assertTrue(exception.getMessage().contains("@BlueEntity"));
    }

    @Test
    void missingBlueKeyFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(MissingKeyEntity.class));

        assertTrue(exception.getMessage().contains("@BlueKey"));
    }

    @Test
    void multipleBlueKeysFail() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(MultipleKeyEntity.class));

        assertTrue(exception.getMessage().contains("multiple @BlueKey"));
    }

    @Test
    void invalidBlueKeyMethodFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(InvalidKeyMethodEntity.class));

        assertTrue(exception.getMessage().contains("must not declare parameters"));
    }

    @Test
    void multipleBlueOptimisticLocksFail() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(MultipleBlueOptimisticLockEntity.class));

        assertTrue(exception.getMessage().contains("only one field"));
    }

    @Test
    void invalidBlueOptimisticLockTypeFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(InvalidBlueOptimisticLockTypeEntity.class));

        assertTrue(exception.getMessage().contains("long or Long"));
    }

    @Test
    void staticBlueOptimisticLockFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(StaticBlueOptimisticLockEntity.class));

        assertTrue(exception.getMessage().contains("must not be static"));
    }

    @Test
    void finalBlueOptimisticLockFails() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(FinalBlueOptimisticLockEntity.class));

        assertTrue(exception.getMessage().contains("must not be final"));
    }

    @Test
    void blueOptimisticLockConflictsWithVersionedFlag() {
        BlueEntityDescriptorFactory factory = new BlueEntityDescriptorFactory();

        BlueDescriptorException exception = assertThrows(BlueDescriptorException.class,
                () -> factory.create(BlueOptimisticLockAndVersionedEntity.class));

        assertTrue(exception.getMessage().contains("cannot be combined"));
    }

    @BlueEntity(collection = "field_entities")
    private static final class FieldKeyEntity {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

        @BlueKey
        private UUID id = ID;
    }

    @BlueEntity(collection = "method_entities")
    private static final class MethodKeyEntity {
        @BlueKey
        private String id() {
            return "method-key";
        }
    }

    @BlueEntity(collection = "custom_codec_entities", codecFactory = CustomCodecFactory.class)
    private static final class CustomCodecEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();
    }

    public static final class CustomCodecFactory implements BlueEntityCodecFactory {
        private static int calls;

        @Override
        public <V> Codec<V> create(Class<V> entityType) {
            calls++;
            return new Codec<V>() {
                @Override
                public byte[] encode(V value) {
                    return new byte[0];
                }

                @Override
                public V decode(byte[] data) {
                    return null;
                }

                @Override
                public String contentType() {
                    return "application/custom";
                }
            };
        }
    }

    @BlueEntity(collection = "indexed_entities")
    private static final class IndexedEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @Indexed
        private String name;
    }

    @BlueEntity(collection = "blue_indexed_entities")
    private static final class BlueIndexedEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueIndex
        private String name;
    }

    @BlueEntity(collection = "blue_descending_indexed_entities")
    private static final class BlueDescendingIndexedEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueIndex(order = BlueIndexHint.Order.DESCENDING)
        private int score;
    }

    @BlueEntity(collection = "blue_nested_indexed_entities")
    private static final class BlueNestedIndexedEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueIndex(path = "guild.id", type = String.class)
        private Guild guild;
    }

    private static final class Guild {
        private String id;
    }

    @BlueEntity(collection = "optimistic_lock_entities")
    private static final class OptimisticLockEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @OptimisticLock
        private Long lockVersion;
    }

    @BlueEntity(collection = "blue_optimistic_lock_entities")
    private static final class BlueOptimisticLockEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private Long lockVersion;
    }

    @BlueEntity(collection = "versioned_entities", versioned = true)
    private static final class VersionedEntity implements Versioned {
        @BlueKey
        private UUID id = UUID.randomUUID();
        private long lockVersion;

        @Override
        public long getLockVersion() {
            return lockVersion;
        }

        @Override
        public void setLockVersion(long version) {
            this.lockVersion = version;
        }
    }

    private static final class MissingEntityAnnotation {
        @BlueKey
        private UUID id = UUID.randomUUID();
    }

    @BlueEntity(collection = "missing_key_entities")
    private static final class MissingKeyEntity {
        private UUID id = UUID.randomUUID();
    }

    @BlueEntity(collection = "multiple_key_entities")
    private static final class MultipleKeyEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueKey
        private String second = "second";
    }

    @BlueEntity(collection = "invalid_key_method_entities")
    private static final class InvalidKeyMethodEntity {
        @BlueKey
        private String id(String prefix) {
            return prefix + "-id";
        }
    }

    @BlueEntity(collection = "multiple_blue_optimistic_lock_entities")
    private static final class MultipleBlueOptimisticLockEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private Long first;

        @BlueOptimisticLock
        private Long second;
    }

    @BlueEntity(collection = "invalid_blue_optimistic_lock_type_entities")
    private static final class InvalidBlueOptimisticLockTypeEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private String lockVersion;
    }

    @BlueEntity(collection = "static_blue_optimistic_lock_entities")
    private static final class StaticBlueOptimisticLockEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private static Long lockVersion;
    }

    @BlueEntity(collection = "final_blue_optimistic_lock_entities")
    private static final class FinalBlueOptimisticLockEntity {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private final Long lockVersion = 0L;
    }

    @BlueEntity(collection = "blue_optimistic_lock_and_versioned_entities", versioned = true)
    private static final class BlueOptimisticLockAndVersionedEntity implements Versioned {
        @BlueKey
        private UUID id = UUID.randomUUID();

        @BlueOptimisticLock
        private Long lockVersion;

        @Override
        public long getLockVersion() {
            return lockVersion == null ? 0L : lockVersion;
        }

        @Override
        public void setLockVersion(long version) {
            this.lockVersion = version;
        }
    }
}
