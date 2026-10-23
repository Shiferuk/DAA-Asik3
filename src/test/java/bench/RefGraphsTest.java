package bench;

import static org.junit.jupiter.api.Assertions.*;

import graph.Graph;
import order.TopologicalSort;
import org.junit.jupiter.api.Test;
import paths.Traversal;

class RefGraphsTest {

    private static void checkWeightsAndNoLoops(Graph g) {
        for (int u = 0; u < g.vertexCount(); u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                assertTrue(e.weight() >= 1 && e.weight() <= 1000);
                assertNotEquals(u, e.to());                                      
            }
        }
    }

    @Test
    void undirectedIsConnectedWithExactlyMEdges() {
        for (int n : new int[] {2, 50, 1000}) {
            Graph g = RefGraphs.undirected(n, 4 * n);
            assertEquals(4 * n, g.edgeCount());
            assertFalse(g.isDirected());
            assertEquals(1, Traversal.weakComponentCount(g));                                        
            checkWeightsAndNoLoops(g);
        }
    }

    @Test
    void directedHasOnlyRandomEdges() {
        Graph g = RefGraphs.directed(1000, 20_000);
        assertEquals(20_000, g.edgeCount());
        assertTrue(g.isDirected());
        checkWeightsAndNoLoops(g);
    }

    @Test
    void dagIsAcyclic() {
        for (int m : new int[] {4_000, 20_000}) {
            Graph g = RefGraphs.dag(1000, m);
            assertEquals(m, g.edgeCount());
            assertTrue(TopologicalSort.kahn(g).isAcyclic());
            checkWeightsAndNoLoops(g);
        }
    }

    @Test
    void sameSeedGivesIdenticalGraphs() {
        assertEquals(RefGraphs.undirected(300, 1200).toString(), RefGraphs.undirected(300, 1200).toString());
        assertEquals(RefGraphs.dag(300, 1200).toString(), RefGraphs.dag(300, 1200).toString());
    }

    @Test
    void generatorFingerprints() {
        assertEquals(-720376156, RefGraphs.undirected(1000, 4000).toString().hashCode());
        assertEquals(-1506784708, RefGraphs.directed(1000, 4000).toString().hashCode());
        assertEquals(690190841, RefGraphs.dag(1000, 4000).toString().hashCode());
    }
}
