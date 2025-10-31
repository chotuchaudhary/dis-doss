package ai.deeprunner.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an index strategy migration record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMigration {
    
    /**
     * Unique migration ID
     */
    private String migrationId;
    
    /**
     * Tenant ID being migrated
     */
    private String tenantId;
    
    /**
     * Document type being migrated
     */
    private String documentType;
    
    /**
     * Source strategy (before migration)
     */
    private String fromStrategy;
    
    /**
     * Target strategy (after migration)
     */
    private String toStrategy;
    
    /**
     * Migration status
     */
    private MigrationStatus status;
    
    /**
     * Old index name (source)
     */
    private String oldIndexName;
    
    /**
     * New index name (target)
     */
    private String newIndexName;
    
    /**
     * Timestamp when migration started
     */
    private LocalDateTime startedAt;
    
    /**
     * Timestamp when migration completed
     */
    private LocalDateTime completedAt;
    
    /**
     * Document counts in old and new indexes
     */
    private DocumentCounts documentCount;
    
    /**
     * Error message if migration failed
     */
    private String errorMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentCounts {
        private Long oldIndex;
        private Long newIndex;
        private Long difference;
    }
    
    /**
     * Migration status enum
     */
    public enum MigrationStatus {
        PENDING,           // Migration scheduled but not started
        IN_PROGRESS,       // Dual-write active, migration in progress
        VERIFYING,         // Data verification in progress
        CUTOVER,           // Reading from new index, still dual-writing
        COMPLETED,         // Migration complete, single-write to new index
        ROLLED_BACK,       // Migration rolled back to old strategy
        FAILED             // Migration failed
    }
}


