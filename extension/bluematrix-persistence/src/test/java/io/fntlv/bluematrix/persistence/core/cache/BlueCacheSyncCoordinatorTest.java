package io.fntlv.bluematrix.persistence.core.cache;

import br.com.finalcraft.everydatabase.Storages;
import br.com.finalcraft.everydatabase.changefeed.ChangeEvent;
import br.com.finalcraft.everydatabase.changefeed.ChangeFeedSupport;
import br.com.finalcraft.everydatabase.changefeed.ChangeListener;
import br.com.finalcraft.everydatabase.changefeed.ChangeSubscription;
import br.com.finalcraft.everydatabase.manager.sync.CacheSyncTransport;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinition;
import io.fntlv.bluematrix.persistence.core.data.definition.BlueDataDefinitionFactory;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueKey;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueCacheSyncCoordinatorTest {

    @Test
    void startBindsOnlyCachedDefinitions() {
        BlueStorage storage = storage(DirectData.class, CachedData.class);
        RecordingTransport transport = new RecordingTransport();
        BlueCacheSyncCoordinator coordinator = new BlueCacheSyncCoordinator();
        BlueDataDefinition<UUID, DirectData> direct = definition(storage, DirectData.class);
        BlueDataDefinition<UUID, CachedData> cached = definition(storage, CachedData.class);

        coordinator.start(storage, transport);
        direct.save(new DirectData(DirectData.ID)).join();
        cached.save(new CachedData(CachedData.ID)).join();

        assertEquals(1, transport.published.size());
        assertEquals("cached_sync_data", transport.published.get(0).collection());
    }

    @Test
    void closeClosesTransport() {
        BlueStorage storage = storage(CachedData.class);
        RecordingTransport transport = new RecordingTransport();
        BlueCacheSyncCoordinator coordinator = new BlueCacheSyncCoordinator();

        coordinator.start(storage, transport);
        coordinator.close();

        assertTrue(transport.closed);
    }

    @SuppressWarnings("unchecked")
    private static <V> BlueDataDefinition<UUID, V> definition(BlueStorage storage, Class<V> dataType) {
        return (BlueDataDefinition<UUID, V>) storage.registry().find(dataType).get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlueStorage storage(Class<?>... dataTypes) {
        BlueStorage storage = new BlueStorage(Storages.createInMemory());
        BlueDataDefinitionFactory factory = new BlueDataDefinitionFactory();
        for (Class dataType : dataTypes) {
            storage.registry().register(factory.create(dataType, storage));
        }
        return storage;
    }

    @BlueEntity(collection = "direct_sync_data")
    private static final class DirectData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

        @BlueKey
        public UUID id;

        private DirectData() {
        }

        private DirectData(UUID id) {
            this.id = id;
        }
    }

    @BlueEntity(collection = "cached_sync_data")
    @BlueCache(policy = BlueCachePolicy.ALWAYS)
    private static final class CachedData {
        private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000051");

        @BlueKey
        public UUID id;

        private CachedData() {
        }

        private CachedData(UUID id) {
            this.id = id;
        }
    }

    private static final class RecordingTransport implements CacheSyncTransport {
        private final List<ChangeEvent> published = new ArrayList<>();
        private final ChangeFeedSupport feed = new ChangeFeedSupport();
        private boolean closed;

        @Override
        public String originId() {
            return "recording";
        }

        @Override
        public void publish(ChangeEvent event) {
            published.add(event);
        }

        @Override
        public ChangeSubscription subscribe(ChangeListener listener) {
            return feed.subscribe(listener);
        }

        @Override
        public void onConnectionStateChanged(Consumer<Boolean> listener) {
            if (listener != null) {
                listener.accept(Boolean.TRUE);
            }
        }

        @Override
        public void close() {
            closed = true;
            feed.closeAll();
        }
    }
}
