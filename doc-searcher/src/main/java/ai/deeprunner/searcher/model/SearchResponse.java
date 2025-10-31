package ai.deeprunner.searcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for document search operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    /**
     * Search results
     */
    private List<SearchResult> results;
    
    /**
     * Total number of documents matching the query
     */
    private Long total;
    
    /**
     * Current page number
     */
    private Integer page;
    
    /**
     * Page size
     */
    private Integer size;
    
    /**
     * Total number of pages
     */
    private Integer totalPages;
    
    /**
     * Search execution time in milliseconds
     */
    private Long tookMs;
}
