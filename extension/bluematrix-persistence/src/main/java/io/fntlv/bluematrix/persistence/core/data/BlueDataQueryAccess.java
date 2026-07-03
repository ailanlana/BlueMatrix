package io.fntlv.bluematrix.persistence.core.data;

import br.com.finalcraft.everydatabase.query.Cursor;
import br.com.finalcraft.everydatabase.query.Page;
import br.com.finalcraft.everydatabase.query.Query;
import br.com.finalcraft.everydatabase.query.QueryOptions;
import br.com.finalcraft.everydatabase.query.Slice;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BlueDataQueryAccess {
    <V> CompletableFuture<List<V>> query(Class<V> dataType, Query query);

    <V> CompletableFuture<List<V>> query(Class<V> dataType, Query query, QueryOptions options);

    <V> CompletableFuture<Long> count(Class<V> dataType, Query query);

    <V> CompletableFuture<Slice<V>> querySlice(Class<V> dataType, Query query, QueryOptions options);

    <V> CompletableFuture<Page<V>> queryPage(Class<V> dataType, Query query, QueryOptions options);

    <V> CompletableFuture<Slice<V>> queryAfter(Class<V> dataType, Query query, Cursor cursor, int limit);
}
