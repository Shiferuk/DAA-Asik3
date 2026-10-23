package scc;

import graph.Graph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import metrics.Metrics;

public final class SccResult {

    private final int count;
    private final int[] componentOf;
    private final Metrics metrics;

    SccResult(int count, int[] componentOf, Metrics metrics) {
        this.count = count;
        this.componentOf = componentOf;
        this.metrics = metrics;
    }

    public int count() { return count; }

    public int componentOf(int v) { return componentOf[v]; }

    public int[] componentIds() { return componentOf.clone(); }

    public List<List<Integer>> components() {
        List<List<Integer>> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new ArrayList<>());
        }
        for (int v = 0; v < componentOf.length; v++) {
            out.get(componentOf[v]).add(v);
        }
        return out;
    }

    public List<List<Integer>> deadlockGroups(Graph g) {
        List<List<Integer>> groups = new ArrayList<>();
        for (List<Integer> c : components()) {
            if (c.size() > 1 || g.hasSelfLoop(c.get(0))) {
                groups.add(c);
            }
        }
        groups.sort(Comparator.comparingInt(c -> c.get(0)));
        return groups;
    }

    public Metrics metrics() { return metrics; }
}
