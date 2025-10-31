package ai.deeprunner.core.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry for named rate limiters.
 * Thread-safe; lazily creates token-bucket limiters with provided config.
 */
@Component
public class RateLimiterRegistry {
    private final ConcurrentMap<String, RateLimiter> nameToLimiter = new ConcurrentHashMap<>();

    public RateLimiter getOrCreate(String name, double permitsPerSecond, int burstCapacity) {
        Objects.requireNonNull(name, "name");
        return nameToLimiter.computeIfAbsent(name, n -> new TokenBucketRateLimiter(permitsPerSecond, burstCapacity));
    }

    public RateLimiter get(String name) {
        return nameToLimiter.get(name);
    }

    public void remove(String name) {
        nameToLimiter.remove(name);
    }

    public void clear() {
        nameToLimiter.clear();
    }
}



