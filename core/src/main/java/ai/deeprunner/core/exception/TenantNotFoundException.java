package ai.deeprunner.core.exception;

public class TenantNotFoundException extends RuntimeException {
    
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
    
    public TenantNotFoundException(String tenantId, Throwable cause) {
        super("Tenant not found: " + tenantId, cause);
    }
}

