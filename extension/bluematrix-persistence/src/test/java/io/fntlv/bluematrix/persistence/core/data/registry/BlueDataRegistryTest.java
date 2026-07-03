package io.fntlv.bluematrix.persistence.core.data.registry;

import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;
import io.fntlv.bluematrix.persistence.core.data.BlueDataRegistry;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueDataRegistryTest {

    @Test
    void registeredDefinitionCanBeFoundByType() {
        BlueDataRegistry registry = new BlueDataRegistry();
        BlueDataDefinition<UUID, FirstData> definition = definition(FirstData.class, UUID.class, "first_data");

        registry.register(definition);

        assertSame(definition, registry.find(FirstData.class).get());
    }

    @Test
    void registeredDefinitionCanBeFoundByCollection() {
        BlueDataRegistry registry = new BlueDataRegistry();
        BlueDataDefinition<UUID, FirstData> definition = definition(FirstData.class, UUID.class, "first_data");

        registry.register(definition);

        assertSame(definition, registry.find(" first_data ").get());
    }

    @Test
    void registeringSameDefinitionIsIdempotent() {
        BlueDataRegistry registry = new BlueDataRegistry();
        BlueDataDefinition<UUID, FirstData> definition = definition(FirstData.class, UUID.class, "first_data");

        registry.register(definition);
        registry.register(definition);

        assertEquals(1, registry.definitions().size());
        assertSame(definition, registry.find(FirstData.class).get());
    }

    @Test
    void duplicateTypeFails() {
        BlueDataRegistry registry = new BlueDataRegistry();
        BlueDataDefinition<UUID, FirstData> first = definition(FirstData.class, UUID.class, "first_data");
        BlueDataDefinition<UUID, FirstData> duplicate = definition(FirstData.class, UUID.class, "other_data");

        registry.register(first);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.register(duplicate));

        assertTrue(exception.getMessage().contains("Duplicate BlueData type"));
    }

    @Test
    void duplicateCollectionFails() {
        BlueDataRegistry registry = new BlueDataRegistry();
        BlueDataDefinition<UUID, FirstData> first = definition(FirstData.class, UUID.class, "shared_data");
        BlueDataDefinition<UUID, SecondData> duplicate = definition(SecondData.class, UUID.class, "shared_data");

        registry.register(first);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.register(duplicate));

        assertTrue(exception.getMessage().contains("Duplicate BlueData collection"));
    }

    @Test
    void definitionsCollectionIsReadOnly() {
        BlueDataRegistry registry = new BlueDataRegistry();
        registry.register(definition(FirstData.class, UUID.class, "first_data"));

        assertThrows(UnsupportedOperationException.class, () -> registry.definitions().clear());
    }

    @Test
    void clearRemovesAllDefinitions() {
        BlueDataRegistry registry = new BlueDataRegistry();
        registry.register(definition(FirstData.class, UUID.class, "first_data"));

        registry.clear();

        assertTrue(registry.definitions().isEmpty());
        assertFalse(registry.find(FirstData.class).isPresent());
        assertFalse(registry.find("first_data").isPresent());
    }

    private static <K, V> BlueDataDefinition<K, V> definition(final Class<V> type,
                                                              final Class<K> keyType,
                                                              final String collection) {
        return new BlueDataDefinition<K, V>() {
            @Override
            public Class<V> type() {
                return type;
            }

            @Override
            public Class<K> keyType() {
                return keyType;
            }

            @Override
            public String collection() {
                return collection;
            }

            @Override
            public K key(V data) {
                return null;
            }

            @Override
            public CompletableFuture<Optional<V>> get(K key) {
                return CompletableFuture.completedFuture(Optional.<V>empty());
            }

            @Override
            public CompletableFuture<List<V>> getAll(Collection<K> keys) {
                return CompletableFuture.completedFuture(Collections.<V>emptyList());
            }

            @Override
            public CompletableFuture<Void> save(V data) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Boolean> delete(K key) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public Repository<K, V> repository() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Ref<K, V> ref(K key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Ref<K, V> ref(K key, CachePolicy policyOverride) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<BatchSaveReport<K>> flushDirty() {
                return CompletableFuture.completedFuture(BatchSaveReport.<K>empty());
            }
        };
    }

    private static final class FirstData {
    }

    private static final class SecondData {
    }
}
