package scc;

import graph.Graph;
import java.util.Arrays;
import metrics.Counter;
import metrics.Metrics;
import util.IntStack;

public final class Kosaraju {

    private Kosaraju() { }

    public static SccResult run(Graph g) {
        int n = g.vertexCount();
        Metrics m = new Metrics();

        int[] finish = new int[n];
        int finished = 0;
        boolean[] seen = new boolean[n];
        int[] next = new int[n];                                                
        IntStack stack = new IntStack();
        for (int s = 0; s < n; s++) {
            if (seen[s]) {
                continue;
            }
            seen[s] = true;
            m.inc(Counter.VERTICES_VISITED);
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.peek();
                if (next[u] < g.outDegree(u)) {
                    int v = g.edgeAt(u, next[u]++).to();
                    m.inc(Counter.EDGES_SCANNED);
                    if (!seen[v]) {
                        seen[v] = true;
                        m.inc(Counter.VERTICES_VISITED);
                        stack.push(v);
                    }
                } else {
                    stack.pop();
                    finish[finished++] = u;
                }
            }
        }
        m.add(Counter.STACK_PUSHES, stack.pushes());
        m.max(Counter.MAX_STACK_DEPTH, stack.maxSize());

        Graph rev = g.reversed();
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int count = 0;
        IntStack stack2 = new IntStack();
        for (int i = n - 1; i >= 0; i--) {
            int s = finish[i];
            if (comp[s] != -1) {
                continue;
            }
            int id = count++;
            comp[s] = id;
            m.inc(Counter.VERTICES_VISITED);
            stack2.push(s);
            while (!stack2.isEmpty()) {
                int u = stack2.pop();
                for (int j = 0; j < rev.outDegree(u); j++) {
                    int v = rev.edgeAt(u, j).to();
                    m.inc(Counter.EDGES_SCANNED);
                    if (comp[v] == -1) {
                        comp[v] = id;
                        m.inc(Counter.VERTICES_VISITED);
                        stack2.push(v);
                    }
                }
            }
        }
        m.add(Counter.STACK_PUSHES, stack2.pushes());
        m.max(Counter.MAX_STACK_DEPTH, stack2.maxSize());
        return new SccResult(count, comp, m);
    }
}
