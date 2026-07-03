package io.fntlv.bluematrix.persistence.core.data.definition;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class DirectBlueDataDefinition<K, V> implements BlueDataDefinition<K, V> {
    private final EntityDescriptor<K, V> descriptor;
    private final Repository<K, V> repository;

    public DirectBlueDataDefinition(EntityDescriptor<K, V> descriptor, Repository<K, V> repository) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        this.descriptor = descriptor;
        this.repository = repository;
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
        return repository.find(key);
    }

    @Override
    public CompletableFuture<List<V>> getAll(Collection<K> keys) {
        return repository.findMany(keys);
    }

    @Override
    public CompletableFuture<Void> save(V data) {
        return repository.save(data);
    }

    @Override
    public CompletableFuture<Boolean> delete(K key) {
        return repository.delete(key);
    }

    @Override
    public Repository<K, V> repository() {
        return repository;
    }

    @Override
    public Ref<K, V> ref(K key) {
        throw unsupportedRef();
    }

    @Override
    public Ref<K, V> ref(K key, CachePolicy policyOverride) {
        throw unsupportedRef();
    }

    @Override
    public CompletableFuture<BatchSaveReport<K>> flushDirty() {
        return CompletableFuture.completedFuture(BatchSaveReport.<K>empty());
    }

    private IllegalArgumentException unsupportedRef() {
        return new IllegalArgumentException("BlueData does not support Ref because it is not cached: "
                + type().getName());
    }
}
