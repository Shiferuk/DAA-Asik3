package paths;

import static org.junit.jupiter.api.Assertions.*;

import bench.RefGraphs;
import bench.Verify;
import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import metrics.Counter;
import org.junit.jupiter.api.Test;
import order.TopologicalSort;

class DagShortestPathTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    @Test
    void pipelineDistancesSourcesAndPaths() throws IOException {
        Graph g = load("jobs_dag.txt");
        PathResult r = DagShortestPath.fromAllSources(g);
        assertEquals(List.of(0), r.sources());
        assertEquals(0, r.source());
        assertArrayEquals(new long[] {0, 3, 2, 3, 8, 9}, r.distances());
        assertEquals(25, r.sumOfDistances());
        assertEquals(List.of(0, 2, 5), r.pathTo(5));
        assertEquals(List.of(0, 2, 3, 4), r.pathTo(4));
        assertEquals(-1, r.parent(0));
        assertEquals(2, r.parent(3));
        Verify.dagDistances("jobs_dag", g, r);
    }

    @Test
    void pathCanBePrinted() throws IOException {
        PathResult r = DagShortestPath.fromAllSources(load("jobs_dag.txt"));
        assertEquals("0 -> 2 -> 5 (distance 9)", r.formatPath(5));
        assertEquals("0 (distance 0)", r.formatPath(0));
        assertEquals("no path to 0", DagShortestPath.fromSource(load("jobs_dag.txt"), 2).formatPath(0));
    }

    @Test
    void everyInDegreeZeroVertexIsASourceAtDistanceZero() {
        Graph g = new Graph(5, true);
        g.addEdge(0, 2, 5);
        g.addEdge(1, 2, 3);
        g.addEdge(2, 3, 1);                                                
        PathResult r = DagShortestPath.fromAllSources(g);
        assertEquals(List.of(0, 1, 4), r.sources().stream().sorted().toList());
        assertEquals(-1, r.source());
        assertArrayEquals(new long[] {0, 0, 3, 4, 0}, r.distances());
        assertEquals(List.of(1, 2, 3), r.pathTo(3));
        assertEquals(List.of(4), r.pathTo(4));
        Verify.dagDistances("two sources", g, r);
    }

    @Test
    void negativeWeightsAreAllowed() {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, -4);
        g.addEdge(0, 2, 3);
        PathResult r = DagShortestPath.fromAllSources(g);
        assertArrayEquals(new long[] {0, 5, 1}, r.distances());
        assertEquals(List.of(0, 1, 2), r.pathTo(2));

        Graph h = new Graph(2, true);
        h.addEdge(0, 1, -7);
        assertEquals(-7, DagShortestPath.fromAllSources(h).distance(1));
        assertEquals(-7, DagShortestPath.fromAllSources(h).sumOfDistances());
    }

    @Test
    void dijkstraBreaksOnNegativeWeightsButDagPathDoesNot() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 1, -3);
        assertThrows(IllegalArgumentException.class, () -> ShortestPaths.dijkstra(g, 0));
        assertEquals(-3, DagShortestPath.fromAllSources(g).distance(1));
    }

    @Test
    void cyclicGraphIsRejected() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.fromAllSources(load("jobs_deadlock.txt")));
        Graph neg = new Graph(2, true);                                                         
        neg.addEdge(0, 1, -1);
        neg.addEdge(1, 0, -1);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.fromAllSources(neg));
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.fromAllSources(loop));
    }

    @Test
    void emptyAndEdgelessGraphs() throws IOException {
        PathResult e = DagShortestPath.fromAllSources(new Graph(0, true));
        assertEquals(0, e.sumOfDistances());
        assertTrue(e.sources().isEmpty());
        PathResult iso = DagShortestPath.fromAllSources(load("jobs_independent.txt"));
        assertEquals(4, iso.sources().size());
        assertEquals(0, iso.sumOfDistances());
    }

    @Test
    void singleSourceLeavesUnreachableAtInfinity() throws IOException {
        PathResult r = DagShortestPath.fromSource(load("jobs_dag.txt"), 2);
        assertEquals(PathResult.INF, r.distance(0));
        assertEquals(PathResult.INF, r.distance(1));
        assertEquals(1, r.distance(3));
        assertEquals(7, r.distance(5));                                                                              
        assertEquals(List.of(2, 5), r.pathTo(5));
        assertTrue(r.pathTo(0).isEmpty());
    }

    @Test
    void counters() {
        Graph g = RefGraphs.dag(1000, 4000);
        PathResult r = DagShortestPath.fromAllSources(g);
        assertEquals(4000, r.metrics().get(Counter.EDGES_SCANNED));                              
        assertEquals(4000, r.metrics().get(Counter.COMPARISONS));                                      
        assertEquals(0, r.metrics().get(Counter.PQ_PUSHES));                              
        assertEquals(0, r.metrics().get(Counter.MAX_STACK_DEPTH));                      
        Verify.dagDistances("ref dag", g, r);
    }

    @Test
    void precomputedOrderGivesSameAnswerAsConvenienceMethod() {
        Graph g = RefGraphs.dag(500, 2000);
        int[] order = TopologicalSort.kahn(g).order();
        assertArrayEquals(DagShortestPath.fromAllSources(g).distances(),
                DagShortestPath.fromAllSources(g, order).distances());
    }

    private static Graph randomSignedDag(Random rnd, int n, int m) {
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = rank[i];
            rank[i] = rank[j];
            rank[j] = t;
        }
        Graph g = new Graph(n, true);
        for (int i = 0; i < m && n >= 2; i++) {
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n - 1);
            if (b >= a) {
                b++;
            }
            int lo = rank[a] < rank[b] ? a : b;
            int hi = lo == a ? b : a;
            g.addEdge(lo, hi, rnd.nextInt(71) - 20);                                                 
        }
        return g;
    }

    private static long[] bellmanFordFromSources(Graph g) {
        int n = g.vertexCount();
        boolean[] hasIn = new boolean[n];
        for (int u = 0; u < n; u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                hasIn[e.to()] = true;
            }
        }
        long[] d = new long[n];
        Arrays.fill(d, PathResult.INF);
        for (int v = 0; v < n; v++) {
            if (!hasIn[v]) {
                d[v] = 0;
            }
        }
        for (int pass = 0; pass < n; pass++) {
            boolean changed = false;
            for (int u = 0; u < n; u++) {
                if (d[u] == PathResult.INF) {
                    continue;
                }
                for (Graph.Edge e : g.neighbors(u)) {
                    if (d[u] + e.weight() < d[e.to()]) {
                        d[e.to()] = d[u] + e.weight();
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return d;
    }

    @Test
    void matchesBellmanFordOnRandomDagsWithNegativeWeights() {
        Random rnd = new Random(21);
        for (int i = 0; i < 200; i++) {
            Graph g = randomSignedDag(rnd, 1 + rnd.nextInt(40), rnd.nextInt(150));
            PathResult r = DagShortestPath.fromAllSources(g);
            assertArrayEquals(bellmanFordFromSources(g), r.distances());
            Verify.dagDistances("signed#" + i, g, r);
            for (int v = 0; v < g.vertexCount(); v++) {                                               
                List<Integer> p = r.pathTo(v);
                long cost = 0;
                for (int k = 0; k + 1 < p.size(); k++) {
                    final int to = p.get(k + 1);
                    final long dp = r.distance(p.get(k));
                    cost += g.neighbors(p.get(k)).stream()
                            .filter(e -> e.to() == to && dp + e.weight() == r.distance(to))
                            .mapToInt(Graph.Edge::weight).findFirst().orElseThrow();
                }
                assertEquals(r.distance(v), cost);
            }
        }
    }

    @Test
    void resultDoesNotDependOnWhichVertexYouPicked() {
        Random rnd = new Random(8);
        for (int i = 0; i < 50; i++) {
            Graph g = randomSignedDag(rnd, 2 + rnd.nextInt(30), rnd.nextInt(100));
            long[] all = DagShortestPath.fromAllSources(g).distances();
            long[] best = new long[g.vertexCount()];
            Arrays.fill(best, PathResult.INF);
            for (int s : DagShortestPath.fromAllSources(g).sources()) {
                long[] d = DagShortestPath.fromSource(g, s).distances();
                for (int v = 0; v < best.length; v++) {
                    best[v] = Math.min(best[v], d[v]);
                }
            }
            assertArrayEquals(best, all);                                                   
        }
    }

    @Test
    void verifierFailsLoudlyOnWrongDistances() {
        Graph a = RefGraphs.dag(300, 900);
        Graph b = RefGraphs.dag(300, 1200);
        PathResult ra = DagShortestPath.fromAllSources(a);
        assertThrows(IllegalStateException.class, () -> Verify.dagDistances("mismatch", b, ra));
    }
}
