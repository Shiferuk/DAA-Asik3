package paths;

import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import metrics.Counter;
import metrics.Metrics;
import util.IntQueue;
import util.IntStack;

public final class Traversal {

    private Traversal() { }

    public static List<Integer> bfs(Graph g, int start) {
        return bfs(g, start, new Metrics());
    }

    public static List<Integer> bfs(Graph g, int start, Metrics m) {
        List<Integer> order = new ArrayList<>();
        boolean[] seen = new boolean[g.vertexCount()];
        IntQueue queue = new IntQueue(g.vertexCount());
        seen[start] = true;
        queue.push(start);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            m.inc(Counter.VERTICES_VISITED);
            for (int i = 0; i < g.outDegree(u); i++) {
                m.inc(Counter.EDGES_SCANNED);
                int v = g.edgeAt(u, i).to();
                if (!seen[v]) {
                    seen[v] = true;
                    queue.push(v);
                }
            }
        }
        m.max(Counter.MAX_QUEUE_SIZE, queue.maxSize());
        return order;
    }

    public static List<Integer> dfs(Graph g, int start) {
        return dfs(g, start, new Metrics());
    }

    public static List<Integer> dfs(Graph g, int start, Metrics m) {
        int n = g.vertexCount();
        List<Integer> order = new ArrayList<>();
        boolean[] seen = new boolean[n];
        int[] next = new int[n];
        IntStack stack = new IntStack();
        seen[start] = true;
        order.add(start);
        m.inc(Counter.VERTICES_VISITED);
        stack.push(start);
        while (!stack.isEmpty()) {
            int u = stack.peek();
            if (next[u] < g.outDegree(u)) {
                m.inc(Counter.EDGES_SCANNED);
                int v = g.edgeAt(u, next[u]++).to();
                if (!seen[v]) {
                    seen[v] = true;
                    order.add(v);
                    m.inc(Counter.VERTICES_VISITED);
                    stack.push(v);
                }
            } else {
                stack.pop();
            }
        }
        m.add(Counter.STACK_PUSHES, stack.pushes());
        m.max(Counter.MAX_STACK_DEPTH, stack.maxSize());
        return order;
    }

    public static int[] weakComponents(Graph g) {
        int n = g.vertexCount();

        int arcs = 0;
        for (int u = 0; u < n; u++) {
            arcs += g.outDegree(u);
        }
        int[] head = new int[n];
        Arrays.fill(head, -1);
        int[] to = new int[2 * arcs];
        int[] nxt = new int[2 * arcs];
        int k = 0;
        for (int u = 0; u < n; u++) {
            for (int i = 0; i < g.outDegree(u); i++) {
                int v = g.edgeAt(u, i).to();
                to[k] = v;
                nxt[k] = head[u];
                head[u] = k++;
                to[k] = u;
                nxt[k] = head[v];
                head[v] = k++;
            }
        }
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int id = 0;
        IntStack stack = new IntStack();
        for (int s = 0; s < n; s++) {
            if (comp[s] != -1) {
                continue;
            }
            comp[s] = id;
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                for (int e = head[u]; e != -1; e = nxt[e]) {
                    if (comp[to[e]] == -1) {
                        comp[to[e]] = id;
                        stack.push(to[e]);
                    }
                }
            }
            id++;
        }
        return comp;
    }

    public static int weakComponentCount(Graph g) {
        int max = -1;
        for (int c : weakComponents(g)) {
            max = Math.max(max, c);
        }
        return max + 1;
    }
}
