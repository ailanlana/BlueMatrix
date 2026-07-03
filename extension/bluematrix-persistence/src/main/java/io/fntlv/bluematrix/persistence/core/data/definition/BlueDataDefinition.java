package io.fntlv.bluematrix.persistence.core.data.definition;

import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BlueDataDefinition<K, V> {
    Class<V> type();

    Class<K> keyType();

    String collection();

    K key(V data);

    CompletableFuture<Optional<V>> get(K key);

    CompletableFuture<List<V>> getAll(Collection<K> keys);

    CompletableFuture<Void> save(V data);

    CompletableFuture<Boolean> delete(K key);

    Repository<K, V> repository();

    Ref<K, V> ref(K key);

    Ref<K, V> ref(K key, CachePolicy policyOverride);

    CompletableFuture<BatchSaveReport<K>> flushDirty();
}
