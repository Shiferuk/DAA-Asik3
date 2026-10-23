package mst;

import java.util.List;
import metrics.Metrics;

public record MstResult(List<MstEdge> edges, long totalWeight, int components, Metrics metrics) {

    public boolean isConnected() {
        return components <= 1;
    }
}
