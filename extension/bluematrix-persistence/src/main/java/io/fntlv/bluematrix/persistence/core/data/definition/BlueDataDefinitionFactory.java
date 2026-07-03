package io.fntlv.bluematrix.persistence.core.data.definition;

import br.com.finalcraft.everydatabase.EntityDescriptor;
import br.com.finalcraft.everydatabase.Repository;
import br.com.finalcraft.everydatabase.codec.Codec;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;
import br.com.finalcraft.everydatabase.manager.CachingManager;
import br.com.finalcraft.everydatabase.manager.cache.CacheOptions;
import io.fntlv.bluematrix.persistence.core.cache.BlueCache;
import io.fntlv.bluematrix.persistence.core.cache.BlueCacheOptionsFactory;
import io.fntlv.bluematrix.persistence.core.cache.BlueCachePolicy;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntity;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntityCodecProvider;
import io.fntlv.bluematrix.persistence.core.descriptor.BlueEntityDescriptorFactory;
import io.fntlv.bluematrix.persistence.core.storage.BlueStorage;

public final class BlueDataDefinitionFactory {
    private final BlueCacheOptionsFactory cacheOptionsFactory;

    public BlueDataDefinitionFactory() {
        this(new BlueCacheOptionsFactory());
    }

    public BlueDataDefinitionFactory(BlueCacheOptionsFactory cacheOptionsFactory) {
        if (cacheOptionsFactory == null) {
            throw new IllegalArgumentException("cacheOptionsFactory cannot be null");
        }
        this.cacheOptionsFactory = cacheOptionsFactory;
    }

    public <K, V> BlueDataDefinition<K, V> create(Class<V> dataType, BlueStorage storage) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType cannot be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        if (isCached(dataType)) {
            return createCached(dataType, storage);
        }
        return createDirect(dataType, storage);
    }

    private boolean isCached(Class<?> dataType) {
        BlueCache cache = dataType.getAnnotation(BlueCache.class);
        return cache != null && cache.policy() != BlueCachePolicy.NO_CACHE;
    }

    private <K, V> DirectBlueDataDefinition<K, V> createDirect(Class<V> dataType, BlueStorage storage) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType cannot be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        BlueEntityCodecProvider repositoryCodecProvider = new BlueEntityCodecProvider() {
            @Override
            public <T> Codec<T> create(Class<T> entityType, BlueEntity entity) {
                return new JacksonJsonCodec<>(entityType);
            }
        };
        EntityDescriptor<K, V> descriptor = new BlueEntityDescriptorFactory(repositoryCodecProvider).create(dataType);
        Repository<K, V> repository = storage.storage().repository(descriptor);
        return new DirectBlueDataDefinition<>(descriptor, repository);
    }

    private <K, V> CachedBlueDataDefinition<K, V> createCached(Class<V> dataType, final BlueStorage storage) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType cannot be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("storage cannot be null");
        }
        BlueEntityCodecProvider refCodecProvider = new BlueEntityCodecProvider() {
            @Override
            public <T> Codec<T> create(Class<T> entityType, BlueEntity entity) {
                return storage.refRegistry().codec(entityType);
            }
        };
        EntityDescriptor<K, V> descriptor = new BlueEntityDescriptorFactory(refCodecProvider).create(dataType);
        CacheOptions cacheOptions = cacheOptionsFactory.create(dataType);
        CachingManager<K, V> manager = storage.refRegistry().manager(descriptor, storage.storage(), cacheOptions);
        return new CachedBlueDataDefinition<>(descriptor, manager, storage.refRegistry());
    }
}
