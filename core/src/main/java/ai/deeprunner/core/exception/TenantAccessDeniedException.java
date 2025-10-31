package ai.deeprunner.core.exception;

public class TenantAccessDeniedException extends RuntimeException {
    
    public TenantAccessDeniedException(String tenantId) {
        super("Access denied to tenant: " + tenantId);
    }
    
    public TenantAccessDeniedException(String tenantId, Throwable cause) {
        super("Access denied to tenant: " + tenantId, cause);
    }
}

