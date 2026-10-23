package metrics;

public enum Counter {
    EDGES_SCANNED(false),
    VERTICES_VISITED(false),
    COMPARISONS(false),
    FIND_CALLS(false),
    UNION_CALLS(false),
    SUCCESSFUL_UNIONS(false),
    PQ_PUSHES(false),
    PQ_POPS(false),
    STALE_POPS(false),
    RELAXATIONS(false),
    STACK_PUSHES(false),
    MAX_STACK_DEPTH(true),
    MAX_QUEUE_SIZE(true);

    private final boolean max;

    Counter(boolean max) { this.max = max; }

    public boolean isMax() { return max; }
}
