package io.fntlv.bluematrix.persistence.core.data.definition;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.CachingManager;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.RefRegistry;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CachedBlueDataDefinition<K, V> implements BlueDataDefinition<K, V> {
    private final EntityDescriptor<K, V> descriptor;
    private final CachingManager<K, V> manager;
    private final RefRegistry refRegistry;

    public CachedBlueDataDefinition(EntityDescriptor<K, V> descriptor,
                                    CachingManager<K, V> manager,
                                    RefRegistry refRegistry) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager cannot be null");
        }
        if (refRegistry == null) {
            throw new IllegalArgumentException("refRegistry cannot be null");
        }
        this.descriptor = descriptor;
        this.manager = manager;
        this.refRegistry = refRegistry;
    }

    @Override
    public Class<V> type() {
        return descriptor.type();
    }

    @Override
    public Class<K> keyType() {
        return descriptor.keyType();
    }

    @Override
    public String collection() {
        return descriptor.collection();
    }

    @Override
    public K key(V data) {
        return descriptor.keyExtractor().apply(data);
    }

    public EntityDescriptor<K, V> descriptor() {
        return descriptor;
    }

    @Override
    public CompletableFuture<Optional<V>> get(K key) {
        return manager.resolveCell(key, manager.defaultPolicy()).thenApply(cell ->
                cell == null ? Optional.<V>empty() : Optional.ofNullable(cell.getValue()));
    }

    @Override
    public CompletableFuture<List<V>> getAll(Collection<K> keys) {
        return manager.getAll(keys);
    }

    @Override
    public CompletableFuture<Void> save(V data) {
        return manager.saveAndCache(data);
    }

    @Override
    public CompletableFuture<Boolean> delete(K key) {
        return manager.deleteAndEvict(key);
    }

    @Override
    public Repository<K, V> repository() {
        return manager.repository();
    }

    @Override
    public Ref<K, V> ref(K key) {
        return refRegistry.ref(key, type());
    }

    @Override
    public Ref<K, V> ref(K key, CachePolicy policyOverride) {
        return refRegistry.ref(key, type(), policyOverride);
    }

    @Override
    public CompletableFuture<BatchSaveReport<K>> flushDirty() {
        return manager.flushDirty();
    }

    public CachingManager<K, V> manager() {
        return manager;
    }
}
