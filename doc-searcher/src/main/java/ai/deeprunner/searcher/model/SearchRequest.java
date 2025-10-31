package ai.deeprunner.searcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request model for document search operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    /**
     * Search query text
     */
    private String query;
    
    /**
     * Fields to search in
     */
    private List<String> fields;
    
    /**
     * Document type filter
     */
    private String documentType;
    
    /**
     * Page number (0-indexed)
     */
    private Integer page;
    
    /**
     * Page size
     */
    private Integer size;
    
    /**
     * Sort fields (format: "field:order" or "field")
     */
    private List<String> sort;
    
    /**
     * Additional filters (field -> value mappings)
     */
    private Map<String, Object> filters;
}
