package metrics;

public final class Metrics {

    private final long[] values = new long[Counter.values().length];

    public void inc(Counter c) { values[c.ordinal()]++; }

    public void add(Counter c, long n) { values[c.ordinal()] += n; }

    public void max(Counter c, long x) {
        if (x > values[c.ordinal()]) {
            values[c.ordinal()] = x;
        }
    }

    public long get(Counter c) { return values[c.ordinal()]; }

    public long dsuOps() { return get(Counter.FIND_CALLS) + get(Counter.UNION_CALLS); }

    public void addAll(Metrics other) {
        for (Counter c : Counter.values()) {
            if (c.isMax()) {
                max(c, other.get(c));
            } else {
                add(c, other.get(c));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Counter c : Counter.values()) {
            if (values[c.ordinal()] != 0) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(c).append('=').append(values[c.ordinal()]);
            }
        }
        return sb.length() == 0 ? "(no work counted)" : sb.toString();
    }
}
