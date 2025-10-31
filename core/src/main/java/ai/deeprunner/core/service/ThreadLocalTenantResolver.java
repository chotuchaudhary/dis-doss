package ai.deeprunner.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class ThreadLocalTenantResolver implements HandlerInterceptor {
    
    private static final ThreadLocal<String> tenantContext = new ThreadLocal<>();
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        
        if (!StringUtils.hasText(tenantId)) {
            log.warn("Tenant ID not found in header {}, using default tenant", TENANT_ID_HEADER);
            tenantId = DEFAULT_TENANT;
        }
        
        tenantContext.set(tenantId);
        log.debug("Set tenant context: {}", tenantId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        tenantContext.remove();
        log.debug("Removed tenant context");
    }
    
    /**
     * Get current tenant ID from thread local context
     * 
     * @return current tenant ID
     */
    public static String getCurrentTenant() {
        String tenantId = tenantContext.get();
        if (tenantId == null) {
            log.warn("Tenant context not set, returning default tenant");
            return DEFAULT_TENANT;
        }
        return tenantId;
    }
    
    /**
     * Set tenant ID in thread local context (for testing)
     * 
     * @param tenantId tenant ID to set
     */
    public static void setCurrentTenant(String tenantId) {
        tenantContext.set(tenantId);
    }
    
    /**
     * Clear tenant context
     */
    public static void clearTenant() {
        tenantContext.remove();
    }
}

