package order;

import graph.Graph;
import metrics.Counter;
import metrics.Metrics;
import util.IntQueue;

public final class TopologicalSort {

    private TopologicalSort() { }

    public static TopoResult kahn(Graph g) {
        requireDirected(g);
        int n = g.vertexCount();
        Metrics m = new Metrics();

        int[] indeg = new int[n];                                                                    
        for (int u = 0; u < n; u++) {
            for (int i = 0; i < g.outDegree(u); i++) {
                indeg[g.edgeAt(u, i).to()]++;
            }
        }
        IntQueue queue = new IntQueue(n);
        for (int v = 0; v < n; v++) {
            if (indeg[v] == 0) {
                queue.push(v);
            }
        }
        int[] order = new int[n];
        int k = 0;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order[k++] = u;
            m.inc(Counter.VERTICES_VISITED);
            for (int i = 0; i < g.outDegree(u); i++) {
                m.inc(Counter.EDGES_SCANNED);
                int v = g.edgeAt(u, i).to();
                if (--indeg[v] == 0) {
                    queue.push(v);
                }
            }
        }
        m.max(Counter.MAX_QUEUE_SIZE, queue.maxSize());

        if (k == n) {
            return new TopoResult(true, order, n, new int[0], m);
        }
        int[] stuck = new int[n - k];
        int s = 0;
        for (int v = 0; v < n; v++) {
            if (indeg[v] > 0) {                                                              
                stuck[s++] = v;
            }
        }
        return new TopoResult(false, null, k, stuck, m);
    }

    public static boolean isValidOrder(Graph g, int[] order) {
        requireDirected(g);
        int n = g.vertexCount();
        if (order == null || order.length != n) {
            return false;
        }
        int[] pos = new int[n];
        java.util.Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            int v = order[i];
            if (v < 0 || v >= n || pos[v] != -1) {
                return false;
            }
            pos[v] = i;
        }
        for (int u = 0; u < n; u++) {
            for (int i = 0; i < g.outDegree(u); i++) {
                if (pos[u] >= pos[g.edgeAt(u, i).to()]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void requireDirected(Graph g) {
        if (!g.isDirected()) {
            throw new IllegalArgumentException("topological order needs a directed graph");
        }
    }
}
