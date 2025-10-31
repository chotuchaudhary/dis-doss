package ai.deeprunner.indexer.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for indexing documents in Elasticsearch with dynamic index resolution
 * Supports both shared indexes (multiple tenants) and isolated indexes (one tenant per index)
 * Strategy can vary per tenant - one tenant can use shared, another can use isolated
 */
@Service
@Slf4j
public class DocumentIndexService {
    
    private final ElasticsearchIndexer elasticsearchIndexer;
    
    public DocumentIndexService(ElasticsearchIndexer elasticsearchIndexer) {
        this.elasticsearchIndexer = elasticsearchIndexer;
    }
    
    /**
     * Index a document with dynamic index resolution
     * 
     * @param tenantId tenant ID
     * @param documentId document ID
     * @param documentType document type (e.g., "document", "metadata", etc.)
     * @param document document content as map
     * @throws IOException if Elasticsearch operation fails
     */
    public void indexDocument(String tenantId, String documentId, String documentType, Map<String, Object> document) throws IOException {
        // Ensure tenantId is in document for filtering in shared mode
        document.put("tenantId", tenantId);
        // Ensure soft-delete flag exists
        if (!document.containsKey("is_deleted")) {
            document.put("is_deleted", false);
        }
        
        // Alias identified by tenantId
        String aliasName = String.format("%s-%s-write", tenantId, documentType);
        // Ensure alias exists and points to backing index
        log.info("Indexing document {} via alias {} ->(tenant: {}, docType: {})",
            documentId, aliasName,  tenantId, documentType);
        
        // Write using alias
        elasticsearchIndexer.indexDocument(aliasName, documentId, document);
    }
    
    /**
     * Index a document with default type "document"
     * 
     * @param tenantId tenant ID
     * @param documentId document ID
     * @param document document content as map
     * @throws IOException if Elasticsearch operation fails
     */
    public void indexDocument(String tenantId, String documentId, Map<String, Object> document) throws IOException {
        indexDocument(tenantId, documentId, "document", document);
    }
    
    /**
     * Delete a document with dynamic index resolution
     * 
     * @param tenantId tenant ID
     * @param documentId document ID
     * @param documentType document type
     * @throws IOException if Elasticsearch operation fails
     */
    public void deleteDocument(String tenantId, String documentType, String documentId) throws IOException {
        // Compute write and read aliases for current period
        String writeAlias = String.format("%s-%s-write", tenantId, documentType);
        String readAlias  = String.format("%s-%s-read",  tenantId, documentType);

        // Delete from write alias first
        log.info("Deleting document {} via WRITE alias {} ->(tenant: {}, docType: {})", documentId, writeAlias, tenantId, documentType);
        try {
            elasticsearchIndexer.deleteDocument(writeAlias, documentId);
            return;
        } catch (IOException e) {
            log.warn("Delete from write alias failed for document {} (alias: {}): {}", documentId, writeAlias, e.getMessage());
        }

        // Then delete from read alias
        log.info("Deleting document {} via READ alias {} ->(tenant: {}, docType: {})", documentId, readAlias, tenantId, documentType);
        try {
            elasticsearchIndexer.deleteDocument(readAlias, documentId);
        } catch (IOException e) {
            log.warn("Delete from read alias failed for document {} (alias: {}): {}", documentId, readAlias, e.getMessage());
            throw new RuntimeException("No document find with id " + documentId);
        }
    }
    
    /**
     * Delete a document with default type "document"
     * 
     * @param tenantId tenant ID
     * @param documentId document ID
     * @throws IOException if Elasticsearch operation fails
     */
    public void deleteDocument(String tenantId, String documentId) throws IOException {
        deleteDocument(tenantId, documentId, "document");
    }
}

