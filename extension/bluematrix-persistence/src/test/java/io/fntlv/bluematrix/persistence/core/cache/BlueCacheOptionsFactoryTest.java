package io.fntlv.bluematrix.persistence.core.cache;

import br.com.finalcraft.everydatabase.manager.cache.CacheEntry;
import br.com.finalcraft.everydatabase.manager.cache.CacheOptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueCacheOptionsFactoryTest {

    @Test
    void missingAnnotationUsesDefaults() {
        BlueCacheOptionsFactory factory = new BlueCacheOptionsFactory();

        CacheOptions options = factory.create(UncachedEntity.class);

        assertTrue(options.policy().cacheable());
        assertEquals(1000, options.maxSize());
        assertFalse(factory.preload(UncachedEntity.class));
    }

    @Test
    void alwaysPolicyCreatesAlwaysCache() {
        BlueCacheOptionsFactory factory = new BlueCacheOptionsFactory();

        CacheOptions options = factory.create(AlwaysCachedEntity.class);

        assertTrue(options.policy().cacheable());
        assertEquals(50, options.maxSize());
        assertTrue(factory.preload(AlwaysCachedEntity.class));
    }

    @Test
    void ttlPolicyCreatesTtlCache() {
        BlueCacheOptionsFactory factory = new BlueCacheOptionsFactory();

        CacheOptions options = factory.create(TtlCachedEntity.class);

        assertTrue(options.policy().cacheable());
        assertEquals(25, options.maxSize());
        assertFalse(options.policy().isFresh(new CacheEntry<Object>(new Object(), Instant.now().minus(Duration.ofSeconds(10)))));
    }

    @Test
    void noCachePolicyCreatesNonCacheablePolicy() {
        BlueCacheOptionsFactory factory = new BlueCacheOptionsFactory();

        CacheOptions options = factory.create(NoCacheEntity.class);

        assertFalse(options.policy().cacheable());
    }

    private static final class UncachedEntity {
    }

    @BlueCache(policy = BlueCachePolicy.ALWAYS, maxSize = 50, preload = true)
    private static final class AlwaysCachedEntity {
    }

    @BlueCache(policy = BlueCachePolicy.TTL, ttlSeconds = 5, maxSize = 25)
    private static final class TtlCachedEntity {
    }

    @BlueCache(policy = BlueCachePolicy.NO_CACHE)
    private static final class NoCacheEntity {
    }
}
