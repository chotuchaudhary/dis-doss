package ai.deeprunner.indexer.consumer;

import ai.deeprunner.indexer.command.CreateDocumentCommand;
import ai.deeprunner.indexer.command.DeleteDocumentCommand;
import ai.deeprunner.indexer.command.UpdateDocumentCommand;
import ai.deeprunner.indexer.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Functional consumers for document commands using Spring Cloud Stream 4.x functional model
 * Compatible with Java 21
 * 
 * These beans are automatically registered as Cloud Stream consumers
 * based on their names: documentCreate, documentUpdate, documentDelete
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CommandHandlers {
    
    private final DocumentIndexService documentIndexService;
    
    /**
     * Consumer for create document commands
     * Automatically wired to documentCreate-in-0 channel
     */
    @Bean
    public Consumer<CreateDocumentCommand> documentCreate() {
        return command -> {
            try {
                log.info("Received create command for document: {} from tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
                
                // Build document map for Elasticsearch
                Map<String, Object> document = buildDocumentFromCommand(command);
                
                // Get document type from category or default to "document"
                String documentType = Optional.ofNullable(command.getDocumentType()).orElse("document");
                
                // Index in Elasticsearch with dynamic index resolution
                documentIndexService.indexDocument(
                    command.getTenantId(), 
                    command.getDocumentId(), 
                    documentType,
                    document
                );
                
                log.info("Successfully indexed document: {} for tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
            } catch (Exception e) {
                log.error("Error processing create command for document: {} in tenant: {}", 
                    command.getDocumentId(), command.getTenantId(), e);
                throw new RuntimeException("Failed to process create command", e);
            }
        };
    }
    
    /**
     * Consumer for update document commands
     * Automatically wired to documentUpdate-in-0 channel
     */
    @Bean
    public Consumer<UpdateDocumentCommand> documentUpdate() {
        return command -> {
            try {
                log.info("Received update command for document: {} from tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
                
                // Build document map for Elasticsearch
                Map<String, Object> document = buildDocumentForUpdate(command);
                
                // Get document type from category or default to "document"
                String documentType = command.getCategory() != null ? command.getCategory() : "document";
                
                // Update in Elasticsearch (index will upsert) with dynamic index resolution
                documentIndexService.indexDocument(
                    command.getTenantId(), 
                    command.getDocumentId(), 
                    documentType,
                    document
                );
                
                log.info("Successfully updated document in Elasticsearch: {} for tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
            } catch (Exception e) {
                log.error("Error processing update command for document: {} in tenant: {}", 
                    command.getDocumentId(), command.getTenantId(), e);
                throw new RuntimeException("Failed to process update command", e);
            }
        };
    }
    
    /**
     * Consumer for delete document commands
     * Automatically wired to documentDelete-in-0 channel
     */
    @Bean
    public Consumer<DeleteDocumentCommand> documentDelete() {
        return command -> {
            try {
                log.info("Received delete command for document: {} from tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
                
                // Delete from Elasticsearch with default type "document"
                // Note: In production, you might want to store document type in the command
                documentIndexService.deleteDocument(
                    command.getTenantId(), 
                    command.getDocumentId(),
                    "document" // Default to "document" type
                );
                
                log.info("Successfully deleted document from Elasticsearch: {} for tenant: {}", 
                    command.getDocumentId(), command.getTenantId());
            } catch (Exception e) {
                log.error("Error processing delete command for document: {} in tenant: {}", 
                    command.getDocumentId(), command.getTenantId(), e);
                throw new RuntimeException("Failed to process delete command", e);
            }
        };
    }
    
    private Map<String, Object> buildDocumentFromCommand(CreateDocumentCommand command) {
        Map<String, Object> document = new HashMap<>(command.getDocument());
        document.put("documentId", command.getDocumentId());
        document.put("tenantId", command.getTenantId());

        return document;
    }
    
    private Map<String, Object> buildDocumentForUpdate(UpdateDocumentCommand command) {
        Map<String, Object> document = new HashMap<>();
        document.put("documentId", command.getDocumentId());
        document.put("tenantId", command.getTenantId());
        document.put("title", command.getTitle());
        document.put("content", command.getContent());
        document.put("category", command.getCategory());
//        document.put("indexedAt", java.time.LocalDateTime.now());
        
        // Add metadata if present
        if (command.getMetadata() != null && !command.getMetadata().isEmpty()) {
            document.put("metadata", command.getMetadata());
        }
        
        return document;
    }
}

