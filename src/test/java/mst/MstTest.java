package mst;

import static org.junit.jupiter.api.Assertions.*;

import bench.Generators;
import bench.Verify;
import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import metrics.Counter;
import org.junit.jupiter.api.Test;

class MstTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    private static MstResult both(String name, Graph g) {
        MstResult k = Mst.kruskal(g);
        MstResult p = Mst.prim(g);
        Verify.mstAgreement(name, g, k, p);
        return k;
    }

    @Test
    void roadNetworkKnownAnswer() throws IOException {
        MstResult r = both("roads_connected", load("roads_connected.txt"));
        assertEquals(33, r.totalWeight());                          
        assertEquals(1, r.components());
        assertEquals(5, r.edges().size());
        assertTrue(r.isConnected());
    }

    @Test
    void disconnectedRoadsGiveForest() throws IOException {
        MstResult r = both("roads_disconnected", load("roads_disconnected.txt"));
        assertEquals(9, r.totalWeight());                                                             
        assertEquals(4, r.components());
        assertEquals(3, r.edges().size());
        assertFalse(r.isConnected());
    }

    @Test
    void directedGraphsAreTreatedAsUndirected() throws IOException {
        assertEquals(12, both("dag", load("jobs_dag.txt")).totalWeight());                          
        MstResult d = both("deadlock", load("jobs_deadlock.txt"));
        assertEquals(5, d.totalWeight());
        assertEquals(3, d.components());                             
    }

    @Test
    void noEdgesAndTinyGraphs() throws IOException {
        MstResult none = both("independent", load("jobs_independent.txt"));
        assertEquals(0, none.totalWeight());
        assertEquals(4, none.components());
        assertTrue(none.edges().isEmpty());
        assertEquals(0, both("empty", new Graph(0, false)).components());
        assertEquals(1, both("single", new Graph(1, false)).components());
    }

    @Test
    void onlySelfLoopsAndParallelEdges() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 0, 5);
        g.addEdge(1, 1, -2);
        g.addEdge(0, 1, 9);
        g.addEdge(0, 1, 3);
        g.addEdge(1, 0, 7);
        MstResult r = both("loops+parallel", g);
        assertEquals(3, r.totalWeight());
        assertEquals(2, r.components());
    }

    @Test
    void negativeWeightsAndEqualWeights() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 1, -5);
        g.addEdge(1, 2, -1);
        g.addEdge(0, 2, 4);
        assertEquals(-6, both("negative", g).totalWeight());

        Graph k4 = new Graph(4, false);
        for (int u = 0; u < 4; u++) {
            for (int v = u + 1; v < 4; v++) {
                k4.addEdge(u, v, 7);
            }
        }
        assertEquals(21, both("K4 equal", k4).totalWeight());                                      
    }

    @Test
    void primFollowsDirectedEdgesBackwards() {
        Graph g = new Graph(2, true);
        g.addEdge(1, 0, 6);
        MstResult r = both("reverse", g);
        assertEquals(6, r.totalWeight());
        assertEquals(1, r.components());
    }

    @Test
    void randomGraphsAgree() {
        Random rnd = new Random(7);
        for (int i = 0; i < 200; i++) {
            both("random#" + i, Generators.randomUndirected(rnd, 1 + rnd.nextInt(40), rnd.nextInt(120)));
        }
    }

    @Test
    void metricsAreCounted() throws IOException {
        Graph g = load("roads_connected.txt");
        MstResult k = Mst.kruskal(g);
        assertEquals(9, k.metrics().get(Counter.EDGES_SCANNED));
        assertEquals(5, k.metrics().get(Counter.SUCCESSFUL_UNIONS));
        assertTrue(k.metrics().get(Counter.COMPARISONS) > 0);
        assertTrue(k.metrics().get(Counter.FIND_CALLS) >= 2 * 9);
        MstResult p = Mst.prim(g);
        assertEquals(p.metrics().get(Counter.PQ_PUSHES), p.metrics().get(Counter.PQ_POPS));
        assertEquals(6, p.metrics().get(Counter.VERTICES_VISITED));
        assertTrue(p.metrics().get(Counter.MAX_QUEUE_SIZE) >= 1);
    }

    @Test
    void verifierFailsWhenWeightsDisagree() throws IOException {
        Graph g = load("roads_disconnected.txt");
        MstResult good = Mst.kruskal(g);
        MstResult bad = new MstResult(good.edges(), good.totalWeight() + 1, good.components(), good.metrics());
        assertThrows(IllegalStateException.class, () -> Verify.mstAgreement("x", g, good, bad));
    }

    @Test
    void verifierFailsWhenComponentCountWrong() throws IOException {
        Graph g = load("roads_disconnected.txt");
        MstResult good = Mst.kruskal(g);
        MstResult bad = new MstResult(good.edges(), good.totalWeight(), good.components() + 1, good.metrics());
        assertThrows(IllegalStateException.class, () -> Verify.mstAgreement("x", g, bad, bad));
    }

    @Test
    void verifierFailsOnCyclicResult() {
        Graph g = new Graph(3, false);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(0, 2, 1);
        MstResult cyc = new MstResult(
                List.of(new MstEdge(0, 1, 1), new MstEdge(1, 2, 1), new MstEdge(0, 2, 1)), 3, 1,
                new metrics.Metrics());
        assertThrows(IllegalStateException.class, () -> Verify.mstAgreement("x", g, cyc, cyc));
    }
}
