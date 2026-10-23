package bench;

import graph.Graph;
import java.util.Random;

public final class Generators {

    private Generators() { }

    public static Graph randomUndirected(Random rnd, int n, int m) {
        Graph g = new Graph(n, false);
        for (int i = 0; i < m; i++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(100));
        }
        return g;
    }

    public static Graph randomConnected(Random rnd, int n, int m) {
        Graph g = new Graph(n, false);
        for (int i = 1; i < n; i++) {
            g.addEdge(rnd.nextInt(i), i, 1 + rnd.nextInt(100));                                               
        }
        for (int e = n - 1; e < m; e++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(100));                                 
        }
        return g;
    }

    public static Graph randomDirected(Random rnd, int n, int m) {
        Graph g = new Graph(n, true);
        for (int i = 0; i < m; i++) {
            g.addEdge(rnd.nextInt(n), rnd.nextInt(n), 1 + rnd.nextInt(100));
        }
        return g;
    }

    public static Graph randomDag(Random rnd, int n, int m) {
        int[] label = new int[n];
        for (int i = 0; i < n; i++) {
            label[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = label[i];
            label[i] = label[j];
            label[j] = t;
        }
        Graph g = new Graph(n, true);
        for (int i = 0; i < m; i++) {
            int a = rnd.nextInt(n);
            int b = rnd.nextInt(n - 1);
            if (b >= a) {
                b++;
            }
            int lo = Math.min(a, b);
            int hi = Math.max(a, b);
            g.addEdge(label[lo], label[hi], 1 + rnd.nextInt(100));
        }
        return g;
    }

    public static Graph chain(int n, boolean closeCycle) {
        Graph g = new Graph(n, true);
        for (int i = 0; i + 1 < n; i++) {
            g.addEdge(i, i + 1, 1);
        }
        if (closeCycle && n > 0) {
            g.addEdge(n - 1, 0, 1);
        }
        return g;
    }

    public static Graph star(int n) {
        Graph g = new Graph(n, true);
        for (int i = 1; i < n; i++) {
            g.addEdge(0, i, 1);
        }
        return g;
    }
}
