package ai.deeprunner.searcher.service;

import ai.deeprunner.searcher.cache.InMemorySearchCache;
import org.springframework.beans.factory.annotation.Value;
import ai.deeprunner.searcher.model.SearchRequest;
import ai.deeprunner.searcher.model.SearchResult;
import ai.deeprunner.searcher.model.SearchResponse;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for searching documents in Elasticsearch
 * Supports dynamic index resolution based on tenant strategy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final SearchQueryBuilder queryBuilder;
    private final InMemorySearchCache searchCache;
    
    @Value("${elasticsearch.index.shared-prefix:documents}")
    private String sharedIndexPrefix;
    
    /**
     * Search documents by query
     *
     * @param tenantId tenant ID
     * @param searchRequest search request with query, filters, pagination
     * @return search response with results
     * @throws IOException if Elasticsearch operation fails
     */
    public SearchResponse searchDocuments(String tenantId, SearchRequest searchRequest) throws IOException {
        String documentType = searchRequest.getDocumentType() != null ? searchRequest.getDocumentType() : "document";
        
        // Use alias identified by tenantId

        List<String> searchFields = new ArrayList<>();

        Query finalQuery = searchFields.isEmpty()
            ? queryBuilder.buildQuery(searchRequest, tenantId)
            : queryBuilder.buildQueryWithFields(searchRequest, tenantId, searchFields);

        String aliasName = String.format("%s-%s-read", tenantId, documentType);
        log.info("Final query for tenant {}: {}", tenantId, finalQuery);
        co.elastic.clients.elasticsearch.core.SearchRequest.Builder esSearchRequestBuilder =
            queryBuilder.buildSearchRequest(finalQuery, searchRequest).index(aliasName);

        // Cache lookup
        String cacheKey = buildCacheKey(tenantId, searchRequest);
        SearchResponse cached = searchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Execute search
        long startTime = System.currentTimeMillis();
        co.elastic.clients.elasticsearch.core.SearchResponse<Map> esResponse = 
            elasticsearchClient.search(esSearchRequestBuilder.build(), Map.class);

        long tookMs = System.currentTimeMillis() - startTime;
        
        // Convert results
        List<SearchResult> results = new ArrayList<>();
        for (Hit<Map> hit : esResponse.hits().hits()) {
            SearchResult result = new SearchResult();
            result.setDocumentId(hit.id());
            result.setScore(hit.score());
            result.setSource(hit.source());
            result.setIndex(hit.index());
            results.add(result);
        }
        
        long total = esResponse.hits().total().value();
        int totalPages = (int) Math.ceil((double) total / searchRequest.getSize());
        
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResults(results);
        searchResponse.setTotal(total);
        searchResponse.setPage(searchRequest.getPage());
        searchResponse.setSize(searchRequest.getSize());
        searchResponse.setTotalPages(totalPages);
        searchResponse.setTookMs(tookMs);
        
        log.info("Search completed - Found {} documents in {}ms", total, tookMs);

        if (total < 50) {
            searchCache.put(cacheKey, searchResponse);
        }
        
        return searchResponse;
    }

    public Object getActiveDoc(String tenantId, String documentType, String documentId) {
        Query finalQuery = Query.of(q -> q
            .bool(b -> b
                .must(m -> m.term(t -> t.field("_id").value(documentId)))
                .filter(f -> f.term(t -> t.field("is_deleted").value(false)))
            ));

        String alias = String.format("%s-%s-read", tenantId, documentType);

        log.info("Final query for tenant {}: {}", tenantId, finalQuery);
        co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder =
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder()
                        .query(finalQuery)
                        .index(alias)
                        .from(0)
                        .size(1);

        try {
            co.elastic.clients.elasticsearch.core.SearchResponse<Map> esResponse =
                    elasticsearchClient.search(builder.build(), Map.class);
            if (esResponse.hits().hits().isEmpty()) throw  new RuntimeException("Document Not Found");
            return esResponse.hits().hits().get(0).source();
        } catch (RuntimeException e) {
            throw e;
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a document by ID
     *
     * @param tenantId tenant ID
     * @param documentId document ID
     * @param documentType document type
     * @return document as map, or null if not found
     * @throws IOException if Elasticsearch operation fails
     */
    public Map<String, Object> getDocumentById(String tenantId, String documentId, String documentType) throws IOException {
        if (documentType == null || documentType.trim().isEmpty()) {
            documentType = "document";
        }
        
        // Use alias identified by tenantId
        String aliasName = String.format("%s-%s-read", tenantId, documentType);
        
        log.info("Fetching document {} from index: {} for tenant: {}", documentId, aliasName, tenantId);
        
        try {
            co.elastic.clients.elasticsearch.core.GetRequest getRequest = 
                co.elastic.clients.elasticsearch.core.GetRequest.of(g -> g
                .index(aliasName)
                .id(documentId)
            );
            
            var response = elasticsearchClient.get(getRequest, Map.class);
            
            if (response.found()) {
                Map<String, Object> document = response.source();
                log.debug("Document found: {}", documentId);
                return document;
            } else {
                log.warn("Document not found: {} in index: {}", documentId, aliasName);
                throw new RuntimeException("Document Not Found");
            }
        } catch (Exception e) {
            log.error("Error fetching document {} from index {}", documentId, aliasName, e);
            throw new RuntimeException("Document Not Found");
        }
    }

    /**
     * Overload: get document by alias index
     */
    public Map<String, Object> getDocumentById(String tenantId, String documentId, String documentType, String alias) throws IOException {
        if (alias != null && !alias.trim().isEmpty()) {
            String indexName = alias.trim();
            log.info("Fetching document {} from alias index: {} for tenant: {}", documentId, indexName, tenantId);
            try {
                co.elastic.clients.elasticsearch.core.GetRequest getRequest =
                    co.elastic.clients.elasticsearch.core.GetRequest.of(g -> g
                    .index(indexName)
                    .id(documentId)
                );

                var response = elasticsearchClient.get(getRequest, Map.class);

                if (response.found()) {
                    return response.source();
                }
                return null;
            } catch (Exception e) {
                throw new IOException("Failed to fetch document by alias", e);
            }
        }
        return getDocumentById(tenantId, documentId, documentType);
    }
    
    private String buildCacheKey(String tenantId, SearchRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append(tenantId == null ? "" : tenantId).append('|');
        sb.append(req.getDocumentType() == null ? "" : req.getDocumentType()).append('|');
        sb.append(req.getQuery() == null ? "" : req.getQuery()).append('|');
        sb.append(req.getPage()).append('|').append(req.getSize()).append('|');
        if (req.getSort() != null) {
            for (String s : req.getSort()) sb.append(s).append(',');
        }
        sb.append('|');
        if (req.getFilters() != null) {
            req.getFilters().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append('=').append(String.valueOf(e.getValue())).append(','));
        }
        return sb.toString();
    }
}
