package ai.deeprunner.searcher.service;

import ai.deeprunner.searcher.model.SearchRequest;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builder for constructing Elasticsearch search queries
 * Breaks down query building into smaller, focused methods
 */
@Component
@Slf4j
public class SearchQueryBuilder {
    
    /**
     * Build a complete Elasticsearch query from search request
     * 
     * @param searchRequest search request
     * @param tenantId tenant ID
     * @return complete Elasticsearch query
     */
    public Query buildQuery(ai.deeprunner.searcher.model.SearchRequest searchRequest, String tenantId) {
        List<Query> mustClauses = new ArrayList<>();
        
        // 1. Build base query (text search or match all)
        Query baseQuery = buildBaseQuery(searchRequest);
        mustClauses.add(baseQuery);
        
        // 2. Add tenant filter if needed (for shared indexes)
        Query tenantFilter = buildTenantFilter(tenantId);
        if (tenantFilter != null) {
            mustClauses.add(tenantFilter);
        }
        
        // 3. Add additional filters if provided
        List<Query> filterQueries = buildFilterQueries(searchRequest);
        mustClauses.addAll(filterQueries);
        
        // 4. Build final query combining all clauses
        return buildFinalQuery(mustClauses);
    }

    /**
     * Build a complete Elasticsearch query with explicit search fields
     *
     * @param searchRequest search request
     * @param tenantId tenant ID
     * @param indexName target index name
     * @param strategy index strategy (shared/isolated)
     * @param searchFields fields to search for text (if empty, defaults are used)
     * @return complete Elasticsearch query
     */
    public Query buildQueryWithFields(ai.deeprunner.searcher.model.SearchRequest searchRequest,
                                      String tenantId,
                                      List<String> searchFields) {
        List<Query> mustClauses = new ArrayList<>();
        
        // 1. Build base query (text search or match all) using provided fields
        Query baseQuery = buildBaseQueryWithFields(searchRequest, searchFields);
        mustClauses.add(baseQuery);
        
        // 2. Add tenant filter if needed (for shared indexes)
        Query tenantFilter = buildTenantFilter(tenantId);
        if (tenantFilter != null) {
            mustClauses.add(tenantFilter);
        }
        
        // 3. Add additional filters if provided
        List<Query> filterQueries = buildFilterQueries(searchRequest);
        mustClauses.addAll(filterQueries);
        
        // 4. Build final query combining all clauses
        return buildFinalQuery(mustClauses);
    }
    
    /**
     * Build base query - either text search or match all
     * 
     * @param searchRequest search request
     * @return base query
     */
    public Query buildBaseQuery(ai.deeprunner.searcher.model.SearchRequest searchRequest) {
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().trim().isEmpty()) {
            return buildTextSearchQuery(searchRequest.getQuery(), searchRequest.getFields());
        } else {
            return buildMatchAllQuery();
        }
    }
    
    /**
     * Build text search query (searches in title and content fields)
     * 
     * @param queryText search query text
     * @return text search query
     */
    public Query buildTextSearchQuery(String queryText, List<String> fields) {
        // Default fields
        return buildTextSearchQueryWithFields(queryText, fields);
    }

    /**
     * Build text search query using provided fields
     */
    public Query buildTextSearchQueryWithFields(String queryText, List<String> fields) {
        List<String> effectiveFields = (fields == null || fields.isEmpty()) 
            ? List.of("title", "content") 
            : fields;
        return Query.of(q -> q
            .bool(b -> {
                for (String field : effectiveFields) {
                    b.should(s -> s.match(MatchQuery.of(m -> m.field(field).query(queryText))));
                }
                b.minimumShouldMatch("1");
                return b;
            })
        );
    }

    /**
     * Build base query with explicit fields
     */
    public Query buildBaseQueryWithFields(ai.deeprunner.searcher.model.SearchRequest searchRequest,
                                          List<String> fields) {
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().trim().isEmpty()) {
            return buildTextSearchQueryWithFields(searchRequest.getQuery(), fields);
        } else {
            return buildMatchAllQuery();
        }
    }
    
    /**
     * Build match all query (returns all documents)
     * 
     * @return match all query
     */
    public Query buildMatchAllQuery() {
        return Query.of(q -> q.matchAll(m -> m));
    }
    
    /**
     * Build tenant filter query (needed for shared indexes)
     * 
     * @param tenantId tenant ID
     * @return tenant filter query, or null if not needed
     */
    public Query buildTenantFilter(String tenantId) {
            return Query.of(q -> q
                .term(TermQuery.of(t -> t
                    .field("tenantId")
                    .value(tenantId)
                ))
            );
    }
    
    /**
     * Build filter queries from additional filters in search request
     * 
     * @param searchRequest search request
     * @return list of filter queries
     */
    public List<Query> buildFilterQueries(ai.deeprunner.searcher.model.SearchRequest searchRequest) {
        List<Query> filterQueries = new ArrayList<>();
        
        if (searchRequest.getFilters() != null && !searchRequest.getFilters().isEmpty()) {
            for (Map.Entry<String, Object> entry : searchRequest.getFilters().entrySet()) {
                Query filterQuery = buildTermFilter(entry.getKey(), entry.getValue());
                filterQueries.add(filterQuery);
            }
        }

        Query filterQuery = buildTermFilter("is_deleted", false);
        filterQueries.add(filterQuery);

        return filterQueries;
    }
    
    /**
     * Build a term filter query (exact match)
     * 
     * @param field field name
     * @param value field value
     * @return term filter query
     */
    public Query buildTermFilter(String field, Object value) {
        return Query.of(q -> q
            .term(TermQuery.of(t -> t
                .field(field)
                .value(value.toString())
            ))
        );
    }
    
    /**
     * Build final query by combining all must clauses
     * If only one clause, return it directly; otherwise combine with bool query
     * 
     * @param mustClauses list of queries that must match
     * @return final combined query
     */
    public Query buildFinalQuery(List<Query> mustClauses) {
        if (mustClauses.isEmpty()) {
            return buildMatchAllQuery();
        }
        
        if (mustClauses.size() == 1) {
            return mustClauses.get(0);
        }
        
        // Combine multiple queries with bool query
        return Query.of(q -> q
            .bool(b -> {
                for (Query mustQuery : mustClauses) {
                    b.must(mustQuery);
                }
                return b;
            })
        );
    }
    
    /**
     * Build Elasticsearch search request with query, pagination, and sorting
     * 
     * @param indexName target index name
     * @param query Elasticsearch query
     * @param searchRequest search request with pagination and sorting
     * @return Elasticsearch search request builder
     */
    public co.elastic.clients.elasticsearch.core.SearchRequest.Builder buildSearchRequest(Query query,
                                                   ai.deeprunner.searcher.model.SearchRequest searchRequest) {
        co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder = 
            new co.elastic.clients.elasticsearch.core.SearchRequest.Builder()
            .query(query)
            .from(searchRequest.getPage() * searchRequest.getSize())
            .size(searchRequest.getSize());
        
        // Apply sorting
        applySorting(builder, searchRequest);
        
        return builder;
    }
    
    /**
     * Apply sorting to search request builder
     * 
     * @param builder search request builder
     * @param searchRequest search request with sort configuration
     */
    public void applySorting(co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder, 
                             ai.deeprunner.searcher.model.SearchRequest searchRequest) {
        if (searchRequest.getSort() != null && !searchRequest.getSort().isEmpty()) {
            // Apply custom sorting
            for (String sortField : searchRequest.getSort()) {
                SortField sort = parseSortField(sortField);
                builder.sort(s -> s.field(f -> f
                    .field(sort.getField())
                    .order(sort.getOrder())
                ));
            }
        } else {
            // Default sort by score (relevance) descending
            applyDefaultSorting(builder);
        }
    }
    
    /**
     * Parse sort field string into SortField object
     * Format: "field:order" or just "field" (defaults to asc)
     * 
     * @param sortField sort field string
     * @return parsed SortField
     */
    private SortField parseSortField(String sortField) {
        String[] parts = sortField.split(":");
        String field = parts[0];
        String order = parts.length > 1 ? parts[1] : "asc";
        
        SortOrder sortOrder = "desc".equalsIgnoreCase(order) 
            ? SortOrder.Desc 
            : SortOrder.Asc;
        
        return new SortField(field, sortOrder);
    }
    
    /**
     * Apply default sorting (by score descending)
     * 
     * @param builder search request builder
     */
    private void applyDefaultSorting(co.elastic.clients.elasticsearch.core.SearchRequest.Builder builder) {
        builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
    }
    
    /**
     * Check if index name indicates a shared index
     * 
     * @param indexName index name
     * @param tenantId tenant ID
     * @return true if shared index
     */
    private boolean isSharedIndex(String indexName, String tenantId) {
        // Shared index format: documents-{documentType}
        // Isolated index format: documents-{tenantId}-{documentType}
        return !indexName.contains(tenantId + "-") && indexName.contains("-");
    }
    
    /**
     * Helper class for parsed sort field
     */
    private static class SortField {
        private final String field;
        private final SortOrder order;
        
        public SortField(String field, SortOrder order) {
            this.field = field;
            this.order = order;
        }
        
        public String getField() {
            return field;
        }
        
        public SortOrder getOrder() {
            return order;
        }
    }
}

