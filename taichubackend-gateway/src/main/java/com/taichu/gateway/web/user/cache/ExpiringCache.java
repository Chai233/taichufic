package com.taichu.gateway.web.user.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiringCache<K, V> {
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    private final long expirationTimeMillis;

    public ExpiringCache(long expirationTimeMillis) {
        this.expirationTimeMillis = expirationTimeMillis;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        // 每小时清理一次过期数据
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public void remove(K key) {
        cache.remove(key);
    }

    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private class CacheEntry<V> {
        private final V value;
        private final long timestamp;

        public CacheEntry(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public V getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expirationTimeMillis;
        }
    }
} 