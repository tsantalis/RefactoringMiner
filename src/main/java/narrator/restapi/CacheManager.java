package narrator.restapi;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class CacheManager {
    private final Map<String, String> clustersCache = new ConcurrentHashMap<>();
    private final Map<String, String> hierarchyCache = new ConcurrentHashMap<>();

    public String getClusters(String url) {
        return clustersCache.get(url);
    }

    public void putClusters(String url, String clusters) {
        clustersCache.put(url, clusters);
    }

    public String getHierarchy(String url) {
        return hierarchyCache.get(url);
    }

    public void putHierarchy(String url, String hierarchy) {
        hierarchyCache.put(url, hierarchy);
    }

    public void clear() {
        clustersCache.clear();
        hierarchyCache.clear();
    }
}
