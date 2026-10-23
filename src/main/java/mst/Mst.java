package mst;

import dsu.DisjointSetUnion;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import metrics.Counter;
import metrics.Metrics;

public final class Mst {

    private Mst() { }

    public static MstResult kruskal(Graph g) {
        Metrics m = new Metrics();
        MstEdge[] edges = edgeList(g).toArray(new MstEdge[0]);
        mergeSort(edges, new MstEdge[edges.length], 0, edges.length, m);

        DisjointSetUnion dsu = new DisjointSetUnion(g.vertexCount());
        List<MstEdge> tree = new ArrayList<>();
        long total = 0;
        for (MstEdge e : edges) {
            m.inc(Counter.EDGES_SCANNED);
            if (dsu.find(e.u()) != dsu.find(e.v())) {
                dsu.union(e.u(), e.v());
                tree.add(e);
                total += e.weight();
            }
        }
        m.add(Counter.FIND_CALLS, dsu.findCalls());
        m.add(Counter.UNION_CALLS, dsu.unionCalls());
        m.add(Counter.SUCCESSFUL_UNIONS, dsu.successfulUnionCalls());
        return new MstResult(tree, total, dsu.components(), m);
    }

    private static void mergeSort(MstEdge[] a, MstEdge[] tmp, int lo, int hi, Metrics m) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, lo, mid, m);
        mergeSort(a, tmp, mid, hi, m);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            m.inc(Counter.COMPARISONS);
            if (a[j].weight() < a[i].weight()) {
                tmp[k++] = a[j++];
            } else {
                tmp[k++] = a[i++];
            }
        }
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        while (j < hi) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }

    public static MstResult prim(Graph g) {
        int n = g.vertexCount();
        Metrics m = new Metrics();
        Graph und = g.isDirected() ? undirectedCopy(g) : g;                                                  

        boolean[] inTree = new boolean[n];
        PriorityQueue<MstEdge> pq = new PriorityQueue<>((a, b) -> {
            m.inc(Counter.COMPARISONS);
            return Integer.compare(a.weight(), b.weight());
        });
        List<MstEdge> tree = new ArrayList<>();
        long total = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (inTree[start]) {
                continue;
            }
            components++;
            inTree[start] = true;
            m.inc(Counter.VERTICES_VISITED);
            scan(und, start, inTree, pq, m);
            while (!pq.isEmpty()) {
                MstEdge e = pq.poll();                                                     
                m.inc(Counter.PQ_POPS);
                if (inTree[e.v()]) {
                    m.inc(Counter.STALE_POPS);
                    continue;                                           
                }
                inTree[e.v()] = true;
                m.inc(Counter.VERTICES_VISITED);
                tree.add(e);
                total += e.weight();
                scan(und, e.v(), inTree, pq, m);
            }
        }
        return new MstResult(tree, total, components, m);
    }

    private static void scan(Graph g, int x, boolean[] inTree, PriorityQueue<MstEdge> pq, Metrics m) {
        for (int i = 0; i < g.outDegree(x); i++) {
            Graph.Edge e = g.edgeAt(x, i);
            m.inc(Counter.EDGES_SCANNED);
            if (!inTree[e.to()]) {
                pq.add(new MstEdge(x, e.to(), e.weight()));
                m.inc(Counter.PQ_PUSHES);
            }
        }
        m.max(Counter.MAX_QUEUE_SIZE, pq.size());
    }

    static List<MstEdge> edgeList(Graph g) {
        List<MstEdge> edges = new ArrayList<>();
        for (int u = 0; u < g.vertexCount(); u++) {
            for (int i = 0; i < g.outDegree(u); i++) {
                Graph.Edge e = g.edgeAt(u, i);
                if (e.to() == u) {
                    continue;
                }
                if (!g.isDirected() && u > e.to()) {
                    continue;
                }
                edges.add(new MstEdge(u, e.to(), e.weight()));
            }
        }
        return edges;
    }

    private static Graph undirectedCopy(Graph g) {
        Graph u = new Graph(g.vertexCount(), false);
        for (int x = 0; x < g.vertexCount(); x++) {
            for (int i = 0; i < g.outDegree(x); i++) {
                Graph.Edge e = g.edgeAt(x, i);
                if (e.to() != x) {
                    u.addEdge(x, e.to(), e.weight());
                }
            }
        }
        return u;
    }
}
