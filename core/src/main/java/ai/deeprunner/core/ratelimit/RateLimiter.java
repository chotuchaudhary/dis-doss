package ai.deeprunner.core.ratelimit;

/**
 * Simple rate limiter interface supporting token acquisition.
 */
public interface RateLimiter {
    /**
     * Try to acquire a number of permits immediately without waiting.
     * @param permits number of permits requested (>=1)
     * @return true if acquired, false otherwise
     */
    boolean tryAcquire(int permits);

    /**
     * @return configured permits per second
     */
    double getPermitsPerSecond();

    /**
     * @return configured burst capacity
     */
    int getBurstCapacity();
}



