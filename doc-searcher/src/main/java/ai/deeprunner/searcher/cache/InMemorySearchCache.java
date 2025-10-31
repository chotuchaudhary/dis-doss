package ai.deeprunner.searcher.cache;

import ai.deeprunner.searcher.model.SearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple in-memory LRU cache for search responses.
 * Thread-safe via intrinsic synchronization on the map instance.
 */
@Component
public class InMemorySearchCache {

    private final Map<String, SearchResponse> cache;

    public InMemorySearchCache(@Value("${search.cache.max-entries:1000}") int maxEntries) {
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, SearchResponse> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public SearchResponse get(String key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    public void put(String key, SearchResponse value) {
        synchronized (cache) {
            cache.put(key, value);
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }
}



