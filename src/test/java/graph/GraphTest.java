package graph;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraphTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    @Test
    void loadsUndirectedAndDirectedFiles() throws IOException {
        Graph r = load("roads_connected.txt");
        assertEquals(6, r.vertexCount());
        assertEquals(9, r.edgeCount());
        assertFalse(r.isDirected());
        assertTrue(r.hasEdge(0, 1));
        assertTrue(r.hasEdge(1, 0));

        Graph j = load("jobs_deadlock.txt");
        assertTrue(j.isDirected());
        assertEquals(8, j.edgeCount());
        assertTrue(j.hasEdge(2, 0));
        assertFalse(j.hasEdge(0, 2));
    }

    @Test
    void graphWithoutEdgesLoads() throws IOException {
        Graph g = load("jobs_independent.txt");
        assertEquals(4, g.vertexCount());
        assertEquals(0, g.edgeCount());
    }

    @Test
    void parallelEdgesKeptAndUndirectedSelfLoopStoredOnce() throws IOException {
        Graph g = load("roads_disconnected.txt");
        assertEquals(2, g.neighbors(3).size());
        assertEquals(1, g.neighbors(2).stream().filter(e -> e.to() == 2).count());
        assertTrue(g.hasSelfLoop(2));
        assertFalse(g.hasSelfLoop(0));
        assertTrue(g.neighbors(5).isEmpty());
        assertEquals(0, g.outDegree(6));
    }

    @Test
    void reversedFlipsEveryArc() throws IOException {
        Graph g = load("jobs_dag.txt");
        Graph r = g.reversed();
        assertEquals(g.vertexCount(), r.vertexCount());
        assertEquals(g.edgeCount(), r.edgeCount());
        assertTrue(r.isDirected());
        assertTrue(r.hasEdge(1, 0));
        assertFalse(r.hasEdge(0, 1));
        assertEquals(List.of(new Graph.Edge(2, 7), new Graph.Edge(4, 1)), r.neighbors(5));
        assertTrue(g.hasEdge(0, 1));                      
    }

    @Test
    void reversedKeepsSelfLoopsAndParallelEdges() throws IOException {
        Graph r = load("jobs_deadlock.txt").reversed();
        assertTrue(r.hasSelfLoop(5));
        assertTrue(r.hasEdge(0, 2));
        Graph d = new Graph(2, true);
        d.addEdge(0, 1, 1);
        d.addEdge(0, 1, 2);
        assertEquals(2, d.reversed().neighbors(1).size());
    }

    @Test
    void outOfRangeVertexRejected() {
        Graph g = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> g.neighbors(5));
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
    }

    @Test
    void malformedFilesThrowIOException(@TempDir Path tmp) throws IOException {
        assertThrows(IOException.class, () -> Graph.fromFile(write(tmp, "empty.txt", "")));
        assertThrows(IOException.class, () -> Graph.fromFile(write(tmp, "h.txt", "3 1 sideways\n0 1 1\n")));
        assertThrows(IOException.class, () -> Graph.fromFile(write(tmp, "short.txt", "3 2 directed\n0 1 1\n")));
        assertThrows(IOException.class, () -> Graph.fromFile(write(tmp, "range.txt", "2 1 directed\n0 5 1\n")));
        assertThrows(IOException.class, () -> Graph.fromFile(write(tmp, "nan.txt", "2 1 directed\n0 1 x\n")));
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        return Files.writeString(dir.resolve(name), content);
    }
}
