package narrator.mcp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.traverse.TraversalPattern;

public class CacheManager {
    private final Map<String, List<Cluster>> clustersCache = new ConcurrentHashMap<>();
    private final Map<String, List<List<TraversalPattern>>> hierarchyCache = new ConcurrentHashMap<>();

    public List<Cluster> getClusters(String url) {
        return clustersCache.get(url);
    }

    public void putClusters(String url, List<Cluster> clusters) {
        clustersCache.put(url, clusters);
    }

    public List<List<TraversalPattern>> getHierarchy(String url) {
        return hierarchyCache.get(url);
    }

    public void putHierarchy(String url, List<List<TraversalPattern>> hierarchy) {
        hierarchyCache.put(url, hierarchy);
    }

    public void clear() {
        clustersCache.clear();
        hierarchyCache.clear();
    }
}
