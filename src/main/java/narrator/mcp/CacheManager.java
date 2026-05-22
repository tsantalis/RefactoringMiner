package narrator.mcp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.traverse.Leaf;
import narrator.graph.cluster.traverse.TraversalPattern;

public class CacheManager {
    private final Map<String, List<Cluster>> clustersCache = new ConcurrentHashMap<>();
    private final Map<String, TraversalPattern> hierarchyCache = new ConcurrentHashMap<>();

    private final Map<String, List<Leaf>> narrativesCache = new ConcurrentHashMap<>();

    public List<Cluster> getClusters(String url) {
        return clustersCache.get(url);
    }

    public void putClusters(String url, List<Cluster> clusters) {
        clustersCache.put(url, clusters);
    }

    public TraversalPattern getHierarchy(String url) {
        return hierarchyCache.get(url);
    }


    public void putHierarchy(String url, TraversalPattern hierarchy) {
        hierarchyCache.put(url, hierarchy);
    }

    public List<Leaf> getNarrative(String key) {
        return narrativesCache.get(key);
    }

    public void putNarrative(String key, List<Leaf> narrative) {
        narrativesCache.put(key, narrative);
    }

    public void clear() {
        clustersCache.clear();
        hierarchyCache.clear();
        narrativesCache.clear();
    }
}
