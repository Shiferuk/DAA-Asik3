package scc;

import static org.junit.jupiter.api.Assertions.*;

import bench.Generators;
import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import metrics.Counter;
import org.junit.jupiter.api.Test;

class SccTest {

    private static Graph load(String name) throws IOException {
        return Graph.fromFile(Path.of("data", name));
    }

    @Test
    void deadlockFileComponents() throws IOException {
        Graph g = load("jobs_deadlock.txt");
        SccResult r = Kosaraju.run(g);
        assertEquals(5, r.count());
        Set<List<Integer>> comps = new HashSet<>(r.components());
        assertEquals(Set.of(List.of(0, 1, 2), List.of(3, 4), List.of(5), List.of(6), List.of(7)), comps);
        assertEquals(List.of(List.of(0, 1, 2), List.of(3, 4), List.of(5)), r.deadlockGroups(g));
    }

    @Test
    void componentsComeInTopologicalOrderOfCondensation() throws IOException {
        Graph g = load("jobs_deadlock.txt");
        SccResult r = Kosaraju.run(g);
        for (int u = 0; u < g.vertexCount(); u++) {
            for (Graph.Edge e : g.neighbors(u)) {
                assertTrue(r.componentOf(u) <= r.componentOf(e.to()));
            }
        }
        assertTrue(r.componentOf(0) < r.componentOf(3));                         
        assertTrue(r.componentOf(3) < r.componentOf(6));                   
    }

    @Test
    void dagHasOnlySingletons() throws IOException {
        Graph g = load("jobs_dag.txt");
        SccResult r = Kosaraju.run(g);
        assertEquals(6, r.count());
        assertTrue(r.deadlockGroups(g).isEmpty());
    }

    @Test
    void edgeCases() throws IOException {
        assertEquals(0, Kosaraju.run(new Graph(0, true)).count());
        assertEquals(4, Kosaraju.run(load("jobs_independent.txt")).count());
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 1);
        SccResult r = Kosaraju.run(loop);
        assertEquals(1, r.count());
        assertEquals(List.of(List.of(0)), r.deadlockGroups(loop));                              
        Graph par = new Graph(2, true);
        par.addEdge(0, 1, 1);
        par.addEdge(0, 1, 2);
        assertEquals(2, Kosaraju.run(par).count());
    }

    @Test
    void undirectedGraphGivesConnectedComponents() throws IOException {
        assertEquals(4, Kosaraju.run(load("roads_disconnected.txt")).count());
    }

    @Test
    void metricsAreRecorded() throws IOException {
        Graph g = load("jobs_deadlock.txt");
        SccResult r = Kosaraju.run(g);
        assertEquals(16, r.metrics().get(Counter.EDGES_SCANNED));                                  
        assertEquals(16, r.metrics().get(Counter.VERTICES_VISITED));                               
        assertTrue(r.metrics().get(Counter.MAX_STACK_DEPTH) >= 1);
        assertTrue(r.metrics().get(Counter.STACK_PUSHES) >= 8);
    }

    @Test
    void hundredThousandDeepPathNoStackOverflow() {
        int n = 100_000;
        SccResult r = Kosaraju.run(Generators.chain(n, false));
        assertEquals(n, r.count());
        assertEquals(n, r.metrics().get(Counter.MAX_STACK_DEPTH));                                               
    }

    @Test
    void hundredThousandCycleIsOneComponent() {
        int n = 100_000;
        Graph g = Generators.chain(n, true);
        SccResult r = Kosaraju.run(g);
        assertEquals(1, r.count());
        assertEquals(n, r.components().get(0).size());
        assertEquals(n, r.metrics().get(Counter.MAX_STACK_DEPTH));
        assertEquals(1, r.deadlockGroups(g).size());
    }

    private static Throwable runOnSmallStack(Runnable body) throws InterruptedException {
        Throwable[] failure = new Throwable[1];
        Thread t = new Thread(null, () -> {
            try {
                body.run();
            } catch (Throwable e) {
                failure[0] = e;
            }
        }, "small-stack", 128 * 1024);                                                               
        t.start();
        t.join();
        return failure[0];
    }

    @Test
    void pathOf100000VerticesFinishesWithoutStackOverflowOnA128KbStack() throws InterruptedException {
        Throwable failure = runOnSmallStack(() -> {
            SccResult r = Kosaraju.run(Generators.chain(100_000, false));
            if (r.count() != 100_000) {
                throw new AssertionError("expected 100000 components, got " + r.count());
            }
        });
        assertEquals(null, failure);                                                          
    }

    @Test
    void cycleOf100000VerticesFinishesWithoutStackOverflowOnA128KbStack() throws InterruptedException {
        Throwable failure = runOnSmallStack(() -> {
            SccResult r = Kosaraju.run(Generators.chain(100_000, true));
            if (r.count() != 1) {
                throw new AssertionError("expected 1 component, got " + r.count());
            }
        });
        assertEquals(null, failure);
    }

    @Test
    void randomSmallGraphsMatchBruteForceReachability() {
        Random rnd = new Random(11);
        for (int iter = 0; iter < 300; iter++) {
            int n = 1 + rnd.nextInt(20);
            Graph g = Generators.randomDirected(rnd, n, rnd.nextInt(60));
            boolean[][] reach = new boolean[n][n];
            for (int s = 0; s < n; s++) {
                reach[s][s] = true;
                java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
                q.add(s);
                while (!q.isEmpty()) {
                    int u = q.poll();
                    for (Graph.Edge e : g.neighbors(u)) {
                        if (!reach[s][e.to()]) {
                            reach[s][e.to()] = true;
                            q.add(e.to());
                        }
                    }
                }
            }
            SccResult r = Kosaraju.run(g);
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    assertEquals(reach[u][v] && reach[v][u], r.componentOf(u) == r.componentOf(v));
                }
            }
        }
    }
}
