package ai.deeprunner.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare a rate limit for a method or class.
 * This defines configuration only; enforcement can be implemented via AOP or manually via the registry.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Ratelimit {
    /**
     * Unique name for this rate limiter.
     * If empty, an auto-generated name may be used by the caller.
     */
    String name() default "";

    /**
     * Permits per second allowed on average.
     */
    double permitsPerSecond() default 100.0;

    /**
     * Maximum burst capacity (bucket size).
     */
    int burstCapacity() default 200;
}



