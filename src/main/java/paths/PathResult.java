package paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import metrics.Metrics;

public final class PathResult {

    public static final long INF = Long.MAX_VALUE;

    private final List<Integer> sources;
    private final long[] dist;
    private final int[] parent;
    private final Metrics metrics;

    PathResult(List<Integer> sources, long[] dist, int[] parent, Metrics metrics) {
        this.sources = sources;
        this.dist = dist;
        this.parent = parent;
        this.metrics = metrics;
    }

    public int source() { return sources.size() == 1 ? sources.get(0) : -1; }

    public List<Integer> sources() { return sources; }

    public long distance(int v) { return dist[v]; }

    public long[] distances() { return dist.clone(); }

    public int parent(int v) { return parent[v]; }

    public boolean isReachable(int v) { return dist[v] != INF; }

    public long sumOfDistances() {
        long sum = 0;
        for (long d : dist) {
            if (d != INF) {
                sum += d;
            }
        }
        return sum;
    }

    public List<Integer> pathTo(int v) {
        if (!isReachable(v)) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        for (int x = v; x != -1; x = parent[x]) {
            path.add(x);
        }
        Collections.reverse(path);
        return path;
    }

    public String formatPath(int v) {
        List<Integer> p = pathTo(v);
        if (p.isEmpty()) {
            return "no path to " + v;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(p.get(i));
        }
        return sb.append(" (distance ").append(dist[v]).append(')').toString();
    }

    public Metrics metrics() { return metrics; }
}
