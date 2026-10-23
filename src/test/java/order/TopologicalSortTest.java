package order;

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
import scc.Kosaraju;

class TopologicalSortTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    @Test
    void dagGetsValidOrder() throws IOException {
        Graph g = load("jobs_dag.txt");
        TopoResult r = TopologicalSort.kahn(g);
        assertTrue(r.isAcyclic());
        assertFalse(r.hasCycle());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, r.order());
        assertTrue(TopologicalSort.isValidOrder(g, r.order()));
        assertEquals(6, r.processedCount());
        assertTrue(r.stuckVertices().isEmpty());
    }

    @Test
    void cycleIsReportedNotReturnedAsOrder() throws IOException {
        Graph g = load("jobs_deadlock.txt");
        TopoResult r = TopologicalSort.kahn(g);
        assertTrue(r.hasCycle());
        assertEquals(1, r.processedCount());                                                           
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), r.stuckVertices());
        IllegalStateException ex = assertThrows(IllegalStateException.class, r::order);
        assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    void selfLoopCountsAsCycle() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 0, 1);
        g.addEdge(0, 1, 1);
        TopoResult r = TopologicalSort.kahn(g);
        assertTrue(r.hasCycle());
        assertEquals(0, r.processedCount());
    }

    @Test
    void cycleDownstreamOfAcyclicPrefix() {
        Graph g = new Graph(5, true);
        g.addEdge(0, 1, 1);                            
        g.addEdge(1, 2, 1);
        g.addEdge(2, 3, 1);
        g.addEdge(3, 2, 1);                 
        g.addEdge(3, 4, 1);
        TopoResult r = TopologicalSort.kahn(g);
        assertTrue(r.hasCycle());
        assertEquals(2, r.processedCount());
        assertEquals(List.of(2, 3, 4), r.stuckVertices());
    }

    @Test
    void parallelEdgesIsolatedAndEmpty() throws IOException {
        Graph p = new Graph(3, true);
        p.addEdge(0, 1, 1);
        p.addEdge(0, 1, 2);
        TopoResult r = TopologicalSort.kahn(p);
        assertTrue(r.isAcyclic());
        assertTrue(TopologicalSort.isValidOrder(p, r.order()));

        TopoResult iso = TopologicalSort.kahn(load("jobs_independent.txt"));
        assertArrayEquals(new int[] {0, 1, 2, 3}, iso.order());
        assertEquals(4, iso.metrics().get(Counter.MAX_QUEUE_SIZE));

        TopoResult empty = TopologicalSort.kahn(new Graph(0, true));
        assertTrue(empty.isAcyclic());
        assertEquals(0, empty.order().length);
    }

    @Test
    void undirectedGraphRejected() throws IOException {
        Graph g = load("roads_connected.txt");
        assertThrows(IllegalArgumentException.class, () -> TopologicalSort.kahn(g));
        assertThrows(IllegalArgumentException.class, () -> TopologicalSort.isValidOrder(g, new int[6]));
    }

    @Test
    void closingABackEdgeOnARandomDagIsDetectedAsCycle() {
        Random rnd = new Random(4);
        for (int i = 0; i < 100; i++) {
            Graph dag = Generators.randomDag(rnd, 2 + rnd.nextInt(60), 1 + rnd.nextInt(150));
            assertTrue(TopologicalSort.kahn(dag).isAcyclic());
            Graph.Edge first = null;
            int from = -1;
            for (int u = 0; u < dag.vertexCount() && first == null; u++) {
                if (dag.outDegree(u) > 0) {
                    from = u;
                    first = dag.edgeAt(u, 0);
                }
            }
            dag.addEdge(first.to(), from, 1);                                                        
            TopoResult r = TopologicalSort.kahn(dag);
            assertTrue(r.hasCycle());
            assertTrue(r.stuckVertices().contains(from));
            assertTrue(r.stuckVertices().contains(first.to()));
            assertThrows(IllegalStateException.class, r::order);                                   
        }
    }

    @Test
    void validatorAcceptsAndRejects() throws IOException {
        Graph g = load("jobs_dag.txt");
        assertTrue(TopologicalSort.isValidOrder(g, new int[] {0, 2, 1, 3, 4, 5}));                         
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {5, 4, 3, 2, 1, 0}));             
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {1, 0, 2, 3, 4, 5}));                  
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 1, 2, 3, 4}));                 
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 1, 2, 3, 4, 4}));              
        assertFalse(TopologicalSort.isValidOrder(g, new int[] {0, 1, 2, 3, 4, 9}));                 
        assertFalse(TopologicalSort.isValidOrder(g, null));
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 1);
        assertFalse(TopologicalSort.isValidOrder(loop, new int[] {0}));                                         
    }

    @Test
    void hundredThousandChainAndStar() {
        int n = 100_000;
        TopoResult chain = TopologicalSort.kahn(Generators.chain(n, false));
        assertTrue(chain.isAcyclic());
        assertEquals(1, chain.metrics().get(Counter.MAX_QUEUE_SIZE));
        TopoResult star = TopologicalSort.kahn(Generators.star(n));
        assertEquals(n - 1, star.metrics().get(Counter.MAX_QUEUE_SIZE));
        assertTrue(TopologicalSort.isValidOrder(Generators.star(n), star.order()));
        TopoResult ring = TopologicalSort.kahn(Generators.chain(n, true));
        assertTrue(ring.hasCycle());
        assertEquals(n, ring.stuckVertices().size());
    }

    @Test
    void randomDagsAlwaysYieldValidOrders() {
        Random rnd = new Random(3);
        for (int i = 0; i < 100; i++) {
            Graph g = Generators.randomDag(rnd, 2 + rnd.nextInt(200), rnd.nextInt(800));
            TopoResult r = TopologicalSort.kahn(g);
            assertTrue(r.isAcyclic());
            assertTrue(TopologicalSort.isValidOrder(g, r.order()));
        }
    }

    @Test
    void kahnAndKosarajuAgreeOnRandomGraphs() {
        Random rnd = new Random(9);
        for (int i = 0; i < 300; i++) {
            int n = 1 + rnd.nextInt(30);
            Graph g = Generators.randomDirected(rnd, n, rnd.nextInt(n * 2));
            Verify.sccAndTopo("random#" + i, g, Kosaraju.run(g), TopologicalSort.kahn(g));
        }
    }

    @Test
    void agreementOnDataFiles() throws IOException {
        for (String f : List.of("jobs_dag.txt", "jobs_deadlock.txt", "jobs_independent.txt")) {
            Graph g = load(f);
            Verify.sccAndTopo(f, g, Kosaraju.run(g), TopologicalSort.kahn(g));
        }
    }

    @Test
    void verifierFailsLoudlyWhenAlgorithmsDisagree() throws IOException {

        Graph dag = load("jobs_dag.txt");
        Graph dead = load("jobs_deadlock.txt");
        assertThrows(IllegalStateException.class,
                () -> Verify.sccAndTopo("mismatch", dag, Kosaraju.run(dag), TopologicalSort.kahn(dead)));
        assertThrows(IllegalStateException.class,
                () -> Verify.sccAndTopo("mismatch", dead, Kosaraju.run(dead), TopologicalSort.kahn(dag)));
    }
}
