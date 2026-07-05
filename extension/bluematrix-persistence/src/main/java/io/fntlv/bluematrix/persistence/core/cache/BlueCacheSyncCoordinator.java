package io.fntlv.bluematrix.persistence.core.cache;

import br.com.finalcraft.everydatabase.manager.sync.CacheSync;
import br.com.finalcraft.everydatabase.manager.sync.CacheSyncTransport;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.CachedBlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

import java.time.Duration;

public final class BlueCacheSyncCoordinator {
    private final Duration pollInterval;
    private CacheSync cacheSync;
    private CacheSyncTransport transport;

    public BlueCacheSyncCoordinator() {
        this(Duration.ofSeconds(30));
    }

    public BlueCacheSyncCoordinator(Duration pollInterval) {
        if (pollInterval == null) {
            throw new IllegalArgumentException("pollInterval cannot be null");
        }
        this.pollInterval = pollInterval;
    }

    public synchronized void start(BlueStorage storage, CacheSyncTransport transport) {
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        if (transport == null) {
            return;
        }
        if (cacheSync != null) {
            throw new IllegalStateException("Blue cache sync is already started");
        }
        CacheSync sync = CacheSync.auto()
                .via(transport)
                .pollEvery(pollInterval);
        for (BlueDataDefinition<?, ?> definition : storage.registry().definitions()) {
            bindIfCached(sync, definition);
        }
        sync.start();
        this.cacheSync = sync;
        this.transport = transport;
    }

    public synchronized void close() {
        if (cacheSync != null) {
            cacheSync.close();
            cacheSync = null;
        }
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void bindIfCached(CacheSync sync, BlueDataDefinition<?, ?> definition) {
        if (definition instanceof CachedBlueDataDefinition) {
            CachedBlueDataDefinition cached = (CachedBlueDataDefinition) definition;
            sync.bind(cached.manager());
        }
    }
}
