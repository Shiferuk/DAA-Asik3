package dsu;

import static org.junit.jupiter.api.Assertions.*;

import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import paths.Traversal;

class GraphDsuTest {

    private static void check(String file, int expected) throws IOException {
        Graph g = Graph.fromFile(Path.of("data", file));
        DisjointSetUnion d = new DisjointSetUnion(g.vertexCount());
        for (int u = 0; u < g.vertexCount(); u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                d.union(u, e.to());                                                  
            }
        }
        assertEquals(expected, d.components());
        assertEquals(Traversal.weakComponentCount(g), d.components());
        int[] comp = Traversal.weakComponents(g);
        for (int u = 0; u < g.vertexCount(); u++) {
            for (int v = 0; v < g.vertexCount(); v++) {
                assertEquals(comp[u] == comp[v], d.find(u) == d.find(v));
            }
        }
        assertEquals(g.vertexCount() - expected, d.successfulUnionCalls());
    }

    @Test void roadsConnected() throws IOException { check("roads_connected.txt", 1); }
    @Test void roadsDisconnected() throws IOException { check("roads_disconnected.txt", 4); }
    @Test void jobsDag() throws IOException { check("jobs_dag.txt", 1); }
    @Test void jobsDeadlock() throws IOException { check("jobs_deadlock.txt", 3); }
    @Test void noEdges() throws IOException { check("jobs_independent.txt", 4); }
}
