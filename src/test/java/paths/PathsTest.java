package paths;

import static org.junit.jupiter.api.Assertions.*;

import bench.Generators;
import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import metrics.Counter;
import org.junit.jupiter.api.Test;

class PathsTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    @Test
    void bfsAndDfsStayInsideComponent() throws IOException {
        Graph g = load("roads_disconnected.txt");
        assertEquals(List.of(0, 1, 2), Traversal.bfs(g, 0));
        assertEquals(List.of(3, 4), Traversal.bfs(g, 3));
        assertEquals(List.of(5), Traversal.bfs(g, 5));
        assertEquals(List.of(0, 1, 2), Traversal.dfs(g, 0));
        assertEquals(List.of(6), Traversal.dfs(g, 6));
    }

    @Test
    void traversalTerminatesOnCycles() throws IOException {
        Graph g = load("jobs_deadlock.txt");
        assertEquals(List.of(0, 1, 2, 3, 4, 6), Traversal.bfs(g, 0));
        assertEquals(List.of(0, 1, 2, 3, 4, 6), Traversal.dfs(g, 0));
        assertEquals(List.of(5), Traversal.dfs(g, 5));
    }

    @Test
    void dfsOnDeepChainUsesExplicitStack() {
        int n = 100_000;
        var m = new metrics.Metrics();
        List<Integer> order = Traversal.dfs(Generators.chain(n, false), 0, m);
        assertEquals(n, order.size());
        assertEquals(n, m.get(Counter.MAX_STACK_DEPTH));
    }

    @Test
    void weakComponentsOnDisconnectedGraph() throws IOException {
        Graph g = load("roads_disconnected.txt");
        int[] c = Traversal.weakComponents(g);
        assertEquals(c[0], c[1]);
        assertEquals(c[3], c[4]);
        assertNotEquals(c[0], c[3]);
        assertNotEquals(c[5], c[6]);
        assertEquals(4, Traversal.weakComponentCount(g));
        assertEquals(0, Traversal.weakComponentCount(new Graph(0, true)));
    }

    @Test
    void dijkstraKnownRoadNetwork() throws IOException {
        PathResult r = ShortestPaths.dijkstra(load("roads_connected.txt"), 0);
        assertArrayEquals(new long[] {0, 7, 9, 20, 20, 11}, r.distances());
        assertEquals(List.of(0, 2, 5, 4), r.pathTo(4));
        assertEquals(List.of(0), r.pathTo(0));
        assertTrue(r.metrics().get(Counter.PQ_POPS) >= r.metrics().get(Counter.VERTICES_VISITED));
        assertEquals(r.metrics().get(Counter.PQ_PUSHES), r.metrics().get(Counter.PQ_POPS));
    }

    @Test
    void dijkstraUnreachableLoopsAndParallelEdges() throws IOException {
        PathResult r = ShortestPaths.dijkstra(load("roads_disconnected.txt"), 3);
        assertEquals(2, r.distance(4));                                      
        assertFalse(r.isReachable(0));
        assertEquals(PathResult.INF, r.distance(6));
        assertTrue(r.pathTo(6).isEmpty());
        PathResult d = ShortestPaths.dijkstra(load("jobs_deadlock.txt"), 0);
        assertEquals(1, d.distance(1));
        assertEquals(2, d.distance(2));
        assertEquals(3, d.distance(3));
        assertFalse(d.isReachable(5));
    }

    @Test
    void dijkstraRejectsNegativeWeights() {
        Graph g = new Graph(2, true);
        g.addEdge(0, 1, -3);
        assertThrows(IllegalArgumentException.class, () -> ShortestPaths.dijkstra(g, 0));
    }

    @Test
    void dagPathsFastestRouteThroughPipeline() throws IOException {
        PathResult r = DagShortestPath.fromSource(load("jobs_dag.txt"), 0);
        assertArrayEquals(new long[] {0, 3, 2, 3, 8, 9}, r.distances());
        assertEquals(9, r.distance(5));
        assertEquals(0, r.metrics().get(Counter.PQ_PUSHES));                              
    }

    @Test
    void dagPathsAllowNegativeWeightsAndRejectCycles() throws IOException {
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 5);
        g.addEdge(1, 2, -4);
        g.addEdge(0, 2, 3);
        assertEquals(1, DagShortestPath.fromSource(g, 0).distance(2));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.fromSource(load("jobs_deadlock.txt"), 0));
    }

    @Test
    void dagPathsMatchDijkstraOnRandomDags() {
        Random rnd = new Random(5);
        for (int i = 0; i < 50; i++) {
            Graph g = Generators.randomDag(rnd, 2 + rnd.nextInt(60), rnd.nextInt(200));
            int src = rnd.nextInt(g.vertexCount());
            assertArrayEquals(ShortestPaths.dijkstra(g, src).distances(), DagShortestPath.fromSource(g, src).distances());
        }
    }
}
