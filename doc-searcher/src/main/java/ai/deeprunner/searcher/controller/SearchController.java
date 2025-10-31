package ai.deeprunner.searcher.controller;

import ai.deeprunner.core.ratelimit.Ratelimit;
import ai.deeprunner.core.service.ThreadLocalTenantResolver;
import ai.deeprunner.searcher.model.SearchRequest;
import ai.deeprunner.searcher.model.SearchResponse;
import ai.deeprunner.searcher.service.DocumentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for document search operations
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class SearchController {
    
    private final DocumentSearchService documentSearchService;
    
    /**
     * Search documents by query
     * 
     * @param query search query string
     * @param documentType document type filter (optional)
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @return search response with results
     */
    @Ratelimit(name="search", permitsPerSecond = 3, burstCapacity=25)
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "fields") String fields,
            @RequestParam(name = "documentType") String documentType,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        
        try {
            String tenantId = ThreadLocalTenantResolver.getCurrentTenant();
            
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(query);
            searchRequest.setFields(Arrays.asList(fields.split(",")));
            searchRequest.setDocumentType(documentType);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            
            SearchResponse response = documentSearchService.searchDocuments(tenantId, searchRequest);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Advanced search with POST request (supports complex queries and filters)
     * 
     * @param searchRequest search request with query, filters, pagination
     * @return search response with results
     */
    @Ratelimit(name="search", permitsPerSecond = 3, burstCapacity=25)
    @PostMapping
    public ResponseEntity<SearchResponse> searchAdvanced(@RequestBody SearchRequest searchRequest) {
        try {
            String tenantId = ThreadLocalTenantResolver.getCurrentTenant();
            SearchResponse response = documentSearchService.searchDocuments(tenantId, searchRequest);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a document by ID
     * 
     * @param documentId document ID
     * @param documentType document type (optional, defaults to "document")
     * @return document as map
     */
    @Ratelimit(name="fetch", permitsPerSecond = 2, burstCapacity=10)
    @GetMapping("/{documentType}/{documentId}")
    public ResponseEntity<?> getDocument(
            @PathVariable(name = "documentId") String documentId,
            @PathVariable(name = "documentType") String documentType) {

        String tenantId = ThreadLocalTenantResolver.getCurrentTenant();
        Object document = documentSearchService.getActiveDoc(tenantId, documentType, documentId);

        if (document != null) {
            return ResponseEntity.ok(document);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document Search Service is running");
    }
}

