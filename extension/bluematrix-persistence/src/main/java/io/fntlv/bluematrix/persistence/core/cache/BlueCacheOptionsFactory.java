package io.fntlv.bluematrix.persistence.core.cache;

import br.com.finalcraft.everydatabase.manager.cache.CacheOptions;
import br.com.finalcraft.everydatabase.manager.cache.CachePolicy;

import java.time.Duration;

public final class BlueCacheOptionsFactory {
    private static final BlueCachePolicy DEFAULT_POLICY = BlueCachePolicy.ALWAYS;
    private static final int DEFAULT_TTL_SECONDS = 300;
    private static final int DEFAULT_MAX_SIZE = 1000;

    public CacheOptions create(Class<?> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType cannot be null");
        }
        BlueCache cache = entityType.getAnnotation(BlueCache.class);
        BlueCachePolicy policy = resolvePolicy(cache);
        int ttlSeconds = resolveTtlSeconds(cache);
        int maxSize = resolveMaxSize(cache);
        return CacheOptions.builder()
                .policy(toCachePolicy(policy, ttlSeconds))
                .maxSize(maxSize)
                .build();
    }

    public boolean preload(Class<?> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType cannot be null");
        }
        BlueCache cache = entityType.getAnnotation(BlueCache.class);
        return cache != null && cache.preload();
    }

    private BlueCachePolicy resolvePolicy(BlueCache cache) {
        if (cache != null && cache.policy() != BlueCachePolicy.DEFAULT) {
            return cache.policy();
        }
        return DEFAULT_POLICY;
    }

    private int resolveTtlSeconds(BlueCache cache) {
        if (cache != null && cache.ttlSeconds() >= 0) {
            return cache.ttlSeconds();
        }
        return DEFAULT_TTL_SECONDS;
    }

    private int resolveMaxSize(BlueCache cache) {
        if (cache != null && cache.maxSize() > 0) {
            return cache.maxSize();
        }
        return DEFAULT_MAX_SIZE;
    }

    private CachePolicy toCachePolicy(BlueCachePolicy policy, int ttlSeconds) {
        switch (policy) {
            case NO_CACHE:
                return CachePolicy.noCache();
            case TTL:
                return CachePolicy.ttl(Duration.ofSeconds(ttlSeconds));
            case ALWAYS:
            case DEFAULT:
            default:
                return CachePolicy.always();
        }
    }
}
