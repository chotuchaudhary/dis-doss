package ai.deeprunner.core.ratelimit;

/**
 * Thrown when a rate-limited method exceeds its configured limit.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}



