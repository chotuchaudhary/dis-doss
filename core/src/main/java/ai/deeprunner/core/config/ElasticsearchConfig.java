package ai.deeprunner.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * Shared Elasticsearch client configuration for both indexer and searcher modules
 */
@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${elasticsearch.hosts}")
    private List<String> hosts;

    @Value("${elasticsearch.api-key:}")
    private String apiKey;

    private RestClient restClient;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // Parse hosts from configuration
            HttpHost[] httpHosts = hosts.stream()
                    .map(host -> {
                        String[] parts = host.split(":");
                        String hostname = parts[0];
                        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
                        return new HttpHost(hostname, port, "http");
                    })
                    .toArray(HttpHost[]::new);

            // Create REST client builder
            org.elasticsearch.client.RestClientBuilder restClientBuilder = RestClient.builder(httpHosts);

            // Add API key authentication if provided
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(
                            new org.apache.http.impl.client.BasicCredentialsProvider()
                    );
                    return httpClientBuilder;
                });

                restClientBuilder.setDefaultHeaders(
                        new org.apache.http.Header[]{
                                new org.apache.http.message.BasicHeader("Authorization", "ApiKey " + apiKey)
                        }
                );

                log.info("Elasticsearch client configured with API key authentication");
            } else {
                log.info("Elasticsearch client configured without authentication");
            }

            // Build REST client
            restClient = restClientBuilder.build();

            // Create ObjectMapper with Java 8 time support
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // Create JSON mapper
            JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper(objectMapper);

            // Create transport
            RestClientTransport transport = new RestClientTransport(restClient, jsonMapper);

            // Create and return Elasticsearch client
            ElasticsearchClient client = new ElasticsearchClient(transport);

            log.info("Elasticsearch client initialized successfully with hosts: {}", hosts);
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch client", e);
            throw new RuntimeException("Failed to initialize Elasticsearch client", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (restClient != null) {
            try {
                restClient.close();
                log.info("Elasticsearch REST client closed");
            } catch (Exception e) {
                log.error("Error closing Elasticsearch REST client", e);
            }
        }
    }
}
