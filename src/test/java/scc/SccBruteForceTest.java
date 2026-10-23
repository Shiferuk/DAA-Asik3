package scc;

import static org.junit.jupiter.api.Assertions.*;

import bench.Generators;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SccBruteForceTest {

    private static boolean[][] reachability(Graph g) {
        int n = g.vertexCount();
        boolean[][] reach = new boolean[n][n];
        for (int s = 0; s < n; s++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            reach[s][s] = true;
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (Graph.Edge e : g.neighbors(u)) {
                    if (!reach[s][e.to()]) {
                        reach[s][e.to()] = true;
                        queue.add(e.to());
                    }
                }
            }
        }
        return reach;
    }

    private static int bruteForceCount(boolean[][] reach) {
        int n = reach.length;
        boolean[] assigned = new boolean[n];
        int classes = 0;
        for (int v = 0; v < n; v++) {
            if (assigned[v]) {
                continue;
            }
            classes++;
            for (int w = 0; w < n; w++) {
                if (reach[v][w] && reach[w][v]) {
                    assigned[w] = true;
                }
            }
        }
        return classes;
    }

    @Test
    void agreesWithBruteForceOnRandomGraphsUpTo200Vertices() {
        Random rnd = new Random(2024);
        for (int iter = 0; iter < 150; iter++) {
            int n = 1 + rnd.nextInt(200);
            int m = switch (iter % 3) {
                case 0 -> rnd.nextInt(n + 1);                                        
                case 1 -> n + rnd.nextInt(2 * n + 1);                                           
                default -> 3 * n + rnd.nextInt(3 * n);                                      
            };
            Graph g = Generators.randomDirected(rnd, n, m);                                           

            boolean[][] reach = reachability(g);
            SccResult r = Kosaraju.run(g);

            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    assertEquals(reach[u][v] && reach[v][u], r.componentOf(u) == r.componentOf(v));
                }
            }
            assertEquals(bruteForceCount(reach), r.count());
        }
    }

    @Test
    void agreesWithBruteForceOnHandBuiltShapes() {
        Graph twoCycles = new Graph(6, true);                                                      
        twoCycles.addEdge(0, 1, 1);
        twoCycles.addEdge(1, 2, 1);
        twoCycles.addEdge(2, 0, 1);
        twoCycles.addEdge(3, 4, 1);
        twoCycles.addEdge(4, 5, 1);
        twoCycles.addEdge(5, 3, 1);
        twoCycles.addEdge(2, 3, 1);
        assertEquals(2, Kosaraju.run(twoCycles).count());
        assertEquals(bruteForceCount(reachability(twoCycles)), Kosaraju.run(twoCycles).count());
    }
}
