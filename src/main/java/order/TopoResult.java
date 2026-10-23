package order;

import java.util.ArrayList;
import java.util.List;
import metrics.Metrics;

public final class TopoResult {

    private final boolean acyclic;
    private final int[] order;                                           
    private final int processed;                                     
    private final int[] stuck;                                                                
    private final Metrics metrics;

    TopoResult(boolean acyclic, int[] order, int processed, int[] stuck, Metrics metrics) {
        this.acyclic = acyclic;
        this.order = order;
        this.processed = processed;
        this.stuck = stuck;
        this.metrics = metrics;
    }

    public boolean isAcyclic() { return acyclic; }

    public boolean hasCycle() { return !acyclic; }

    public int[] order() {
        if (!acyclic) {
            throw new IllegalStateException("graph has a cycle: only " + processed + " vertices could be ordered, "
                    + stuck.length + " are on a cycle or wait on one");
        }
        return order.clone();
    }

    public int processedCount() { return processed; }

    public List<Integer> stuckVertices() {
        List<Integer> l = new ArrayList<>(stuck.length);
        for (int v : stuck) {
            l.add(v);
        }
        return l;
    }

    public Metrics metrics() { return metrics; }
}
