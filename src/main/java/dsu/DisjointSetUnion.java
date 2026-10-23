package dsu;

import java.util.Arrays;

public final class DisjointSetUnion {

    private int[] parent;
    private int[] size;                             
    private int components;
    private long findCalls;
    private long unionCalls;
    private long successfulUnionCalls;

    public DisjointSetUnion(int n) {
        makeSet(n);
    }

    public void makeSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0, got " + n);
        }
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        components = n;
        findCalls = 0;
        unionCalls = 0;
        successfulUnionCalls = 0;
    }

    public int find(int x) {
        check(x);
        findCalls++;
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[x] != root) {                    
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    public boolean union(int x, int y) {
        unionCalls++;
        int rx = find(x);
        int ry = find(y);
        if (rx == ry) {
            return false;
        }
        if (size[rx] < size[ry]) {                                       
            int t = rx;
            rx = ry;
            ry = t;
        }
        parent[ry] = rx;
        size[rx] += size[ry];
        components--;
        successfulUnionCalls++;
        return true;
    }

    public int components() {
        return components;
    }

    public int elementCount() {
        return parent.length;
    }

    public int setSize(int x) {
        return size[find(x)];
    }

    public long findCalls() {
        return findCalls;
    }

    public long unionCalls() {
        return unionCalls;
    }

    public long successfulUnionCalls() {
        return successfulUnionCalls;
    }

    public void resetCounters() {
        findCalls = 0;
        unionCalls = 0;
        successfulUnionCalls = 0;
    }

    int depth(int x) {
        check(x);
        int d = 0;
        while (parent[x] != x) {
            x = parent[x];
            d++;
        }
        return d;
    }

    private void check(int x) {
        if (x < 0 || x >= parent.length) {
            throw new IllegalArgumentException("Element " + x + " out of range 0.." + (parent.length - 1));
        }
    }

    @Override
    public String toString() {
        return "DSU{n=" + parent.length + ", components=" + components
                + ", finds=" + findCalls + ", unions=" + successfulUnionCalls
                + ", parent=" + Arrays.toString(parent) + "}";
    }
}
