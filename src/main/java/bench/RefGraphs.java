package bench;

import graph.Graph;
import java.util.Random;

public final class RefGraphs {

    private RefGraphs() { }

    public static Graph undirected(int n, int m) {
        Random rnd = new Random(42);
        int[] perm = shuffled(n, rnd);
        Graph g = new Graph(n, false);
        for (int i = 1; i < n; i++) {
            int u = perm[rnd.nextInt(i)];
            int v = perm[i];
            g.addEdge(u, v, weight(rnd));
        }
        for (int e = n - 1; e < m; e++) {
            int a = rnd.nextInt(n);
            int b = otherThan(a, n, rnd);
            g.addEdge(a, b, weight(rnd));
        }
        return g;
    }

    public static Graph directed(int n, int m) {
        Random rnd = new Random(42);
        Graph g = new Graph(n, true);
        for (int e = 0; e < m; e++) {
            int a = rnd.nextInt(n);
            int b = otherThan(a, n, rnd);
            g.addEdge(a, b, weight(rnd));
        }
        return g;
    }

    public static Graph dag(int n, int m) {
        Random rnd = new Random(42);
        int[] rank = shuffled(n, rnd);
        Graph g = new Graph(n, true);
        for (int e = 0; e < m; e++) {
            int a = rnd.nextInt(n);
            int b = otherThan(a, n, rnd);
            int w = weight(rnd);
            if (rank[a] > rank[b]) {
                int t = a;
                a = b;
                b = t;
            }
            g.addEdge(a, b, w);
        }
        return g;
    }

    private static int[] shuffled(int n, Random rnd) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        return p;
    }

    private static int otherThan(int a, int n, Random rnd) {
        int b;
        do {
            b = rnd.nextInt(n);
        } while (b == a);
        return b;
    }

    private static int weight(Random rnd) {
        return 1 + rnd.nextInt(1000);
    }
}
