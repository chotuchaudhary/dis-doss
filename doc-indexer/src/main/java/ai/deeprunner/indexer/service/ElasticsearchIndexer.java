package ai.deeprunner.indexer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Low-level Elasticsearch indexer
 * Performs actual indexing operations on Elasticsearch
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexer {
    
    private final ElasticsearchClient elasticsearchClient;
    
    /**
     * Index a document in Elasticsearch
     * 
     * @param indexName target index name
     * @param documentId document ID
     * @param document document content as map
     * @throws IOException if Elasticsearch operation fails
     */
    public void indexDocument(String indexName, String documentId, Map<String, Object> document) throws IOException {
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(indexName)
                .id(documentId)
                .document(document)
            );
            
            IndexResponse response = elasticsearchClient.index(request);
            
            log.info("Indexed document in Elasticsearch - Index: {}, Document: {}, Version: {}", 
                indexName, documentId, response.version());
        } catch (IOException e) {
            log.error("Failed to index document {} in index {}", documentId, indexName, e);
            throw e;
        }
    }
    
    /**
     * Delete a document from Elasticsearch
     * 
     * @param indexName target index name
     * @param documentId document ID to delete
     * @throws IOException if Elasticsearch operation fails
     */
    public void deleteDocument(String indexName, String documentId) throws IOException {
        try {
            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u
                    .index(indexName)       // index or alias
                    .id(documentId)
                    .script(s -> s
                            .inline(InlineScript.of(is -> is
                                 .source("ctx._source.is_deleted = params.is_deleted") // script
                                 .params(Map.of("is_deleted", JsonData.of(true)))                  // param
                            ))
                    )
            );

            UpdateResponse<Object> updateResponse = elasticsearchClient.update(updateRequest, Object.class);

            log.info("Deleted document from Elasticsearch - Index: {}, Document: {}, Result: {}", 
                indexName, documentId, updateResponse.result());
        } catch (IOException e) {
            log.error("Failed to delete document {} from index {}", documentId, indexName, e);
            throw e;
        }
    }
}

