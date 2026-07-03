package io.fntlv.bluematrix.persistence.core.data;

import br.com.finalcraft.everydatabase.manager.BatchSaveReport;
import br.com.finalcraft.everydatabase.manager.Ref;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;
import br.com.finalcraft.everydatabase.query.Cursor;
import br.com.finalcraft.everydatabase.query.Page;
import br.com.finalcraft.everydatabase.query.Query;
import br.com.finalcraft.everydatabase.query.QueryOptions;
import br.com.finalcraft.everydatabase.query.Slice;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class DefaultBlueDataAccess implements BlueDataAccess, BlueDataQueryAccess {
    private final BlueStorage storage;

    public DefaultBlueDataAccess(BlueStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        this.storage = storage;
    }

    @Override
    public <K, V> CompletableFuture<Optional<V>> get(Class<V> dataType, K key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.get(key);
    }

    @Override
    public <K, V> CompletableFuture<List<V>> getAll(Class<V> dataType, Collection<K> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("keys cannot be null");
        }
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.getAll(keys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> CompletableFuture<Void> save(V data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        Class<V> dataType = (Class<V>) data.getClass();
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.save(data);
    }

    @Override
    public <K, V> CompletableFuture<Boolean> delete(Class<V> dataType, K key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.delete(key);
    }

    @Override
    public <K, V> Ref<K, V> ref(K key, Class<V> dataType) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.ref(key);
    }

    @Override
    public <K, V> Ref<K, V> ref(K key, Class<V> dataType, CachePolicy policyOverride) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if (policyOverride == null) {
            throw new IllegalArgumentException("policyOverride cannot be null");
        }
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.ref(key, policyOverride);
    }

    @Override
    public <K, V> CompletableFuture<BatchSaveReport<K>> flushDirty(Class<V> dataType) {
        BlueDataDefinition<K, V> definition = requireDefinition(dataType);
        return definition.flushDirty();
    }

    @Override
    public CompletableFuture<List<BatchSaveReport<?>>> flushAllDirty() {
        List<CompletableFuture<BatchSaveReport<?>>> futures = new ArrayList<>();
        for (BlueDataDefinition<?, ?> definition : storage.registry().definitions()) {
            futures.add(flushDefinition(definition));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(ignored -> collectReports(futures));
    }

    @Override
    public <V> CompletableFuture<List<V>> query(Class<V> dataType, Query query) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().query(query);
    }

    @Override
    public <V> CompletableFuture<List<V>> query(Class<V> dataType, Query query, QueryOptions options) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().query(query, options);
    }

    @Override
    public <V> CompletableFuture<Long> count(Class<V> dataType, Query query) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().count(query);
    }

    @Override
    public <V> CompletableFuture<Slice<V>> querySlice(Class<V> dataType, Query query, QueryOptions options) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().querySlice(query, options);
    }

    @Override
    public <V> CompletableFuture<Page<V>> queryPage(Class<V> dataType, Query query, QueryOptions options) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().queryPage(query, options);
    }

    @Override
    public <V> CompletableFuture<Slice<V>> queryAfter(Class<V> dataType, Query query, Cursor cursor, int limit) {
        if (query == null) {
            throw new IllegalArgumentException("query cannot be null");
        }
        if (cursor == null) {
            throw new IllegalArgumentException("cursor cannot be null");
        }
        BlueDataDefinition<?, V> definition = requireDefinition(dataType);
        return definition.repository().queryAfter(query, cursor, limit);
    }

    @SuppressWarnings("unchecked")
    private <K, V> BlueDataDefinition<K, V> requireDefinition(Class<V> dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType cannot be null");
        }
        BlueDataDefinition<?, ?> definition = storage.registry().find(dataType)
                .orElseThrow(() -> new IllegalArgumentException("BlueData is not registered: " + dataType.getName()));
        return (BlueDataDefinition<K, V>) definition;
    }

    private CompletableFuture<BatchSaveReport<?>> flushDefinition(BlueDataDefinition<?, ?> definition) {
        return definition.flushDirty().thenApply(report -> report);
    }

    private List<BatchSaveReport<?>> collectReports(List<CompletableFuture<BatchSaveReport<?>>> futures) {
        List<BatchSaveReport<?>> reports = new ArrayList<>(futures.size());
        for (CompletableFuture<BatchSaveReport<?>> future : futures) {
            reports.add(future.join());
        }
        return reports;
    }
}
