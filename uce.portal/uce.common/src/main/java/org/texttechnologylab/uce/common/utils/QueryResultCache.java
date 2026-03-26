package org.texttechnologylab.uce.common.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.texttechnologylab.uce.common.config.CommonConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class QueryResultCache {
    private static final Logger logger = LogManager.getLogger(QueryResultCache.class);
    private static volatile QueryResultCache globalInstance;

    private final Cache<String, Object> cache;
    private final long maxEntries;
    private final boolean enabled;

    private QueryResultCache(CommonConfig config) {
        long configuredEntries = 5000L;
        if (config != null) {
            configuredEntries = Math.max(0L, config.getQueryCacheMaxEntries());
        }
        this.maxEntries = configuredEntries;
        this.enabled = configuredEntries > 0;

        if (!enabled) {
            this.cache = null;
            logger.info("Global query cache disabled (query.cache.max.entries <= 0).");
            return;
        }

        this.cache = Caffeine.newBuilder()
                .maximumSize(configuredEntries)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
        logger.info("Initialized global query cache with maximumSize={} entries.", configuredEntries);
    }

    public static QueryResultCache global(CommonConfig config) {
        if (globalInstance == null) {
            synchronized (QueryResultCache.class) {
                if (globalInstance == null) {
                    globalInstance = new QueryResultCache(config);
                }
            }
        }
        return globalInstance;
    }

    public <T> T getOrLoad(String key, Supplier<T> loader) {
        if (!enabled || cache == null) return loader.get();
        @SuppressWarnings("unchecked")
        var value = (T) cache.get(key, k -> loader.get());
        return value;
    }

    public List<String> getOrLoadStringList(String key, Supplier<List<String>> loader) {
        return getOrLoad(key, () -> List.copyOf(loader.get()));
    }

    public void invalidate(String key) {
        if (!enabled || cache == null) return;
        cache.invalidate(key);
    }

    public long getMaximumEntries() {
        return maxEntries;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
