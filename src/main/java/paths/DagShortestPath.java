package paths;

import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import metrics.Counter;
import metrics.Metrics;
import order.TopoResult;
import order.TopologicalSort;

public final class DagShortestPath {

    private DagShortestPath() { }

    public static PathResult fromAllSources(Graph g) {
        return fromAllSources(g, orderOrThrow(g));
    }

    public static PathResult fromAllSources(Graph g, int[] order) {
        int n = g.vertexCount();
        Metrics m = new Metrics();
        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, PathResult.INF);
        Arrays.fill(parent, -1);
        List<Integer> sources = new ArrayList<>();
        for (int u : order) {
            if (dist[u] == PathResult.INF) {                                                
                dist[u] = 0;
                sources.add(u);
            }
            relaxOutgoing(g, u, dist, parent, m);
        }
        return new PathResult(sources, dist, parent, m);
    }

    public static PathResult fromSource(Graph g, int source) {
        return fromSource(g, source, orderOrThrow(g));
    }

    public static PathResult fromSource(Graph g, int source, int[] order) {
        int n = g.vertexCount();
        Metrics m = new Metrics();
        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, PathResult.INF);
        Arrays.fill(parent, -1);
        dist[source] = 0;
        for (int u : order) {
            if (dist[u] != PathResult.INF) {
                relaxOutgoing(g, u, dist, parent, m);
            }
        }
        return new PathResult(List.of(source), dist, parent, m);
    }

    private static void relaxOutgoing(Graph g, int u, long[] dist, int[] parent, Metrics m) {
        for (int i = 0; i < g.outDegree(u); i++) {
            Graph.Edge e = g.edgeAt(u, i);
            m.inc(Counter.EDGES_SCANNED);
            long nd = dist[u] + e.weight();
            m.inc(Counter.COMPARISONS);
            if (nd < dist[e.to()]) {
                dist[e.to()] = nd;
                parent[e.to()] = u;
                m.inc(Counter.RELAXATIONS);
            }
        }
    }

    private static int[] orderOrThrow(Graph g) {
        TopoResult topo = TopologicalSort.kahn(g);
        if (!topo.isAcyclic()) {
            throw new IllegalArgumentException("graph has a cycle, no topological order: "
                    + topo.stuckVertices().size() + " vertices could not be ordered");
        }
        return topo.order();
    }
}
