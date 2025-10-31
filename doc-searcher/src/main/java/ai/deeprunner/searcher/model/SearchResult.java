package ai.deeprunner.searcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Model representing a single search result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    /**
     * Document ID
     */
    private String documentId;
    
    /**
     * Relevance score
     */
    private Double score;
    
    /**
     * Document source (the actual document data)
     */
    private Map<String, Object> source;
    
    /**
     * Index name where the document was found
     */
    private String index;
}
