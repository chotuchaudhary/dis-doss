package ai.deeprunner.core.ratelimit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe token-bucket rate limiter.
 * Uses wall-clock in milliseconds; refills tokens continuously.
 */
final class TokenBucketRateLimiter implements RateLimiter {
    private final double permitsPerSecond;
    private final int burstCapacity;

    // tokens scaled by 1e6 to keep precision with integer math
    private final AtomicLong tokensMicros = new AtomicLong(0);
    private volatile long lastRefillNanos;

    TokenBucketRateLimiter(double permitsPerSecond, int burstCapacity) {
        if (permitsPerSecond <= 0) throw new IllegalArgumentException("permitsPerSecond must be > 0");
        if (burstCapacity <= 0) throw new IllegalArgumentException("burstCapacity must be > 0");
        this.permitsPerSecond = permitsPerSecond;
        this.burstCapacity = burstCapacity;
        this.lastRefillNanos = System.nanoTime();
        this.tokensMicros.set(burstToMicros(burstCapacity));
    }

    @Override
    public boolean tryAcquire(int permits) {
        if (permits <= 0) throw new IllegalArgumentException("permits must be >= 1");
        refill();
        long required = permitsToMicros(permits);
        while (true) {
            long current = tokensMicros.get();
            if (current < required) return false;
            if (tokensMicros.compareAndSet(current, current - required)) return true;
        }
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos <= 0) return;
        // compute tokens to add
        double permitsToAdd = (elapsedNanos / 1_000_000_000.0) * permitsPerSecond;
        long microsToAdd = permitsToMicros(permitsToAdd);
        if (microsToAdd <= 0) return;
        lastRefillNanos = now;
        long max = burstToMicros(burstCapacity);
        long prev, next;
        do {
            prev = tokensMicros.get();
            next = Math.min(max, prev + microsToAdd);
        } while (!tokensMicros.compareAndSet(prev, next));
    }

    private static long permitsToMicros(double permits) {
        return (long) Math.floor(permits * 1_000_000.0);
    }

    private static long burstToMicros(int burst) {
        return permitsToMicros(burst);
    }

    @Override
    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }

    @Override
    public int getBurstCapacity() {
        return burstCapacity;
    }
}



