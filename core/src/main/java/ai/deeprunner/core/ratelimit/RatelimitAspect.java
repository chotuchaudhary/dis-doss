package ai.deeprunner.core.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import ai.deeprunner.core.service.ThreadLocalTenantResolver;

/**
 * Aspect that enforces @Ratelimit on methods/classes using RateLimiterRegistry.
 * If permits cannot be acquired immediately, throws RateLimitExceededException.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RatelimitAspect {
    
    private final RateLimiterRegistry registry;
    
    @Around("@annotation(ai.deeprunner.core.ratelimit.Ratelimit) || @within(ai.deeprunner.core.ratelimit.Ratelimit)")
    public Object enforceRateLimit(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Ratelimit onMethod = AnnotationUtils.findAnnotation(method, Ratelimit.class);
        Ratelimit onClass = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Ratelimit.class);
        Ratelimit cfg = onMethod != null ? onMethod : onClass;
        if (cfg == null) {
            return pjp.proceed();
        }
        String name = cfg.name();
        if (name == null || name.trim().isEmpty()) {
            name = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
        // Post-tenant resolution: append tenantId if available to isolate limits per-tenant
        String tenantId = ThreadLocalTenantResolver.getCurrentTenant();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            name = name + ":" + tenantId;
        }
        double permitsPerSecond = cfg.permitsPerSecond();
        int burst = cfg.burstCapacity();
        RateLimiter limiter = registry.getOrCreate(name, permitsPerSecond, burst);
        if (!limiter.tryAcquire(1)) {
            String msg = "Rate limit exceeded for " + name + " (pps=" + permitsPerSecond + ", burst=" + burst + ")";
            log.warn(msg);
            throw new RateLimitExceededException(msg);
        }
        return pjp.proceed();
    }
}


