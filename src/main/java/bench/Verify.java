package bench;

import dsu.DisjointSetUnion;
import graph.Graph;
import mst.MstEdge;
import mst.MstResult;
import paths.PathResult;
import order.TopoResult;
import order.TopologicalSort;
import paths.Traversal;
import scc.SccResult;
import util.IntQueue;

public final class Verify {

    private Verify() { }

    public static void mstAgreement(String name, Graph g, MstResult kruskal, MstResult prim) {
        if (kruskal.totalWeight() != prim.totalWeight()) {
            throw fail(name, "total weight differs, Kruskal=" + kruskal.totalWeight()
                    + " Prim=" + prim.totalWeight());
        }
        if (kruskal.components() != prim.components()) {
            throw fail(name, "component count differs, Kruskal=" + kruskal.components()
                    + " Prim=" + prim.components());
        }
        int expected = Traversal.weakComponentCount(g);
        if (kruskal.components() != expected) {
            throw fail(name, "reported " + kruskal.components() + " components but the graph has " + expected);
        }
        checkForest(name, "Kruskal", g, kruskal);
        checkForest(name, "Prim", g, prim);
    }

    private static void checkForest(String name, String algo, Graph g, MstResult r) {
        int n = g.vertexCount();
        if (r.edges().size() != n - r.components()) {
            throw fail(name, algo + " returned " + r.edges().size() + " edges, expected n - components = "
                    + (n - r.components()));
        }
        DisjointSetUnion d = new DisjointSetUnion(n);
        long sum = 0;
        for (MstEdge e : r.edges()) {
            if (!d.union(e.u(), e.v())) {
                throw fail(name, algo + " result contains a cycle at " + e);
            }
            sum += e.weight();
        }
        if (sum != r.totalWeight()) {
            throw fail(name, algo + " edge weights sum to " + sum + " but totalWeight is " + r.totalWeight());
        }
        if (d.components() != r.components()) {
            throw fail(name, algo + " edges leave " + d.components() + " components, reported " + r.components());
        }
    }

    public static void sccAndTopo(String name, Graph g, SccResult scc, TopoResult topo) {
        int n = g.vertexCount();
        for (int u = 0; u < n; u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                if (scc.componentOf(u) > scc.componentOf(e.to())) {
                    throw fail(name, "edge " + u + "->" + e.to() + " goes from component "
                            + scc.componentOf(u) + " to earlier component " + scc.componentOf(e.to()));
                }
            }
        }
        var deadlocks = scc.deadlockGroups(g);
        if (topo.isAcyclic()) {
            if (!deadlocks.isEmpty()) {
                throw fail(name, "Kahn found no cycle but Kosaraju found deadlock groups " + deadlocks);
            }
            if (scc.count() != n) {
                throw fail(name, "acyclic graph must have n singleton SCCs, got " + scc.count() + " for n=" + n);
            }
            if (!TopologicalSort.isValidOrder(g, topo.order())) {
                throw fail(name, "Kahn's order violates pos[u] < pos[v] for some edge");
            }
            return;
        }
        if (deadlocks.isEmpty()) {
            throw fail(name, "Kahn reported a cycle but Kosaraju found no deadlock group");
        }
        if (topo.processedCount() + topo.stuckVertices().size() != n) {
            throw fail(name, "processed + stuck != n");
        }
        boolean[] expectedStuck = reachableFrom(g, deadlocks);
        boolean[] actualStuck = new boolean[n];
        for (int v : topo.stuckVertices()) {
            actualStuck[v] = true;
        }
        for (int v = 0; v < n; v++) {
            if (expectedStuck[v] != actualStuck[v]) {
                throw fail(name, "vertex " + v + (actualStuck[v]
                        ? " is reported stuck but is not reachable from any deadlock group"
                        : " is on/after a cycle but Kahn ordered it"));
            }
        }
    }

    private static boolean[] reachableFrom(Graph g, java.util.List<java.util.List<Integer>> groups) {
        int n = g.vertexCount();
        boolean[] seen = new boolean[n];
        IntQueue q = new IntQueue(n);
        for (var grp : groups) {
            for (int v : grp) {
                if (!seen[v]) {
                    seen[v] = true;
                    q.push(v);
                }
            }
        }
        while (!q.isEmpty()) {
            int u = q.poll();
            for (Graph.Edge e : g.neighbors(u)) {
                if (!seen[e.to()]) {
                    seen[e.to()] = true;
                    q.push(e.to());
                }
            }
        }
        return seen;
    }

    public static void dagDistances(String name, Graph g, PathResult r) {
        int n = g.vertexCount();
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                indeg[e.to()]++;
                if (r.distance(e.to()) > r.distance(u) + e.weight()) {
                    throw fail(name, "edge " + u + "->" + e.to() + " can still relax: dist " + r.distance(e.to())
                            + " > " + r.distance(u) + " + " + e.weight());
                }
            }
        }
        for (int v = 0; v < n; v++) {
            if (!r.isReachable(v)) {
                throw fail(name, "vertex " + v + " is unreachable although every vertex must be reachable");
            }
            if (indeg[v] == 0) {
                if (r.distance(v) != 0 || r.parent(v) != -1) {
                    throw fail(name, "source " + v + " must have distance 0 and parent -1");
                }
                continue;
            }
            int p = r.parent(v);
            if (p < 0) {
                throw fail(name, "non-source vertex " + v + " has no parent");
            }
            boolean ok = false;
            for (Graph.Edge e : g.neighbors(p)) {
                if (e.to() == v && r.distance(p) + e.weight() == r.distance(v)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw fail(name, "parent edge " + p + "->" + v + " does not explain dist[" + v + "]");
            }
        }
    }

    public static void referenceRow(String name, ReferenceTable.Row ref,
                                    long kruskalWeight, long primWeight, long sccCount, long dagSum) {
        if (kruskalWeight != ref.mstWeight() || primWeight != ref.mstWeight()) {
            throw fail(name, "MST weight differs from the reference table: expected " + ref.mstWeight()
                    + ", Kruskal=" + kruskalWeight + ", Prim=" + primWeight);
        }
        if (sccCount != ref.sccCount()) {
            throw fail(name, "SCC count differs from the reference table: expected " + ref.sccCount() + ", got " + sccCount);
        }
        if (dagSum != ref.dagDistanceSum()) {
            throw fail(name, "sum of DAG distances differs from the reference table: expected "
                    + ref.dagDistanceSum() + ", got " + dagSum);
        }
    }

    public static void scanLaws(String name, int m, long kosaraju, long kahn, long dagRelax) {
        if (kosaraju != 2L * m) {
            throw fail(name, "Kosaraju must scan 2m = " + 2L * m + " edges, scanned " + kosaraju);
        }
        if (kahn != m) {
            throw fail(name, "Kahn must scan m = " + m + " edges on a DAG, scanned " + kahn);
        }
        if (dagRelax != m) {
            throw fail(name, "DAG relaxation must scan m = " + m + " edges, scanned " + dagRelax);
        }
    }

    private static IllegalStateException fail(String name, String msg) {
        return new IllegalStateException(name + ": " + msg);
    }
}
