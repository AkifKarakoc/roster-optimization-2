package com.rosteroptimization.service.optimization.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class OptimizationCache {

    private final ConcurrentHashMap<String, Double> fitnessCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> generalCache = new ConcurrentHashMap<>();
    private final int maxSize;

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicInteger evictionCount = new AtomicInteger(0);

    public OptimizationCache() {
        this.maxSize = 5000;
    }

    public OptimizationCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public void cacheFitness(String signature, double fitness) {
        if (fitnessCache.size() >= maxSize) {
            evictOldEntries();
        }
        fitnessCache.put(signature, fitness);
    }

    public Double getFitness(String signature) {
        Double result = fitnessCache.get(signature);
        if (result != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }
        return result;
    }

    public void cacheObject(String key, Object value) {
        if (generalCache.size() >= maxSize) {
            evictOldGeneralEntries();
        }
        generalCache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> type) {
        Object result = generalCache.get(key);
        if (result != null && type.isInstance(result)) {
            hitCount.incrementAndGet();
            return (T) result;
        } else {
            missCount.incrementAndGet();
            return null;
        }
    }

    public void clear() {
        fitnessCache.clear();
        generalCache.clear();
        hitCount.set(0);
        missCount.set(0);
        evictionCount.set(0);
        log.debug("Cache cleared");
    }

    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total == 0 ? 0.0 : (double) hitCount.get() / total;
    }

    public CacheStatistics getStatistics() {
        return new CacheStatistics(
                fitnessCache.size(),
                generalCache.size(),
                hitCount.get(),
                missCount.get(),
                evictionCount.get(),
                getHitRate()
        );
    }

    public void cleanup() {
        clear();
        log.debug("Cache cleanup completed");
    }

    private void evictOldEntries() {
        int toRemove = maxSize / 5;
        fitnessCache.entrySet().removeIf(entry -> Math.random() < 0.2);
        evictionCount.addAndGet(toRemove);
    }

    private void evictOldGeneralEntries() {
        int toRemove = maxSize / 5;
        generalCache.entrySet().removeIf(entry -> Math.random() < 0.2);
        evictionCount.addAndGet(toRemove);
    }

    public record CacheStatistics(
            int fitnessEntries,
            int generalEntries,
            long hitCount,
            long missCount,
            int evictionCount,
            double hitRate
    ) {}
}