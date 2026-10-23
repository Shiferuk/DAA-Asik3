package paths;

import graph.Graph;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import metrics.Counter;
import metrics.Metrics;

public final class ShortestPaths {

    private ShortestPaths() { }

    public static PathResult dijkstra(Graph g, int source) {
        int n = g.vertexCount();
        Metrics m = new Metrics();
        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, PathResult.INF);
        Arrays.fill(parent, -1);
        dist[source] = 0;

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        pq.add(new long[] {0, source});
        m.inc(Counter.PQ_PUSHES);
        m.max(Counter.MAX_QUEUE_SIZE, 1);
        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            m.inc(Counter.PQ_POPS);
            int u = (int) top[1];
            if (top[0] > dist[u]) {
                m.inc(Counter.STALE_POPS);
                continue;
            }
            m.inc(Counter.VERTICES_VISITED);
            for (int i = 0; i < g.outDegree(u); i++) {
                graph.Graph.Edge e = g.edgeAt(u, i);
                m.inc(Counter.EDGES_SCANNED);
                if (e.weight() < 0) {
                    throw new IllegalArgumentException("Dijkstra requires non-negative weights");
                }
                long nd = dist[u] + e.weight();
                if (nd < dist[e.to()]) {
                    dist[e.to()] = nd;
                    parent[e.to()] = u;
                    m.inc(Counter.RELAXATIONS);
                    pq.add(new long[] {nd, e.to()});
                    m.inc(Counter.PQ_PUSHES);
                    m.max(Counter.MAX_QUEUE_SIZE, pq.size());
                }
            }
        }
        return new PathResult(List.of(source), dist, parent, m);
    }
}
