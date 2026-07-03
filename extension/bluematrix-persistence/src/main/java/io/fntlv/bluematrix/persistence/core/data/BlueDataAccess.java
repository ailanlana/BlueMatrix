package io.fntlv.bluematrix.persistence.core.data;

import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BlueDataAccess {
    <K, V> CompletableFuture<Optional<V>> get(Class<V> dataType, K key);

    <K, V> CompletableFuture<List<V>> getAll(Class<V> dataType, Collection<K> keys);

    <V> CompletableFuture<Void> save(V data);

    <K, V> CompletableFuture<Boolean> delete(Class<V> dataType, K key);

    <K, V> Ref<K, V> ref(K key, Class<V> dataType);

    <K, V> Ref<K, V> ref(K key, Class<V> dataType, CachePolicy policyOverride);

    <K, V> CompletableFuture<BatchSaveReport<K>> flushDirty(Class<V> dataType);

    CompletableFuture<List<BatchSaveReport<?>>> flushAllDirty();
}
