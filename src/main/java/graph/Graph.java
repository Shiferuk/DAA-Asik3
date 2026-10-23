package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Graph {

    public record Edge(int to, int weight) { }

    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adj;
    private int edgeCount;

    public Graph(int n, boolean directed) {
        if (n < 0) {
            throw new IllegalArgumentException("Vertex count must be >= 0, got " + n);
        }
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
    }

    public void addEdge(int u, int v, int w) {
        checkVertex(u);
        checkVertex(v);
        adj.get(u).add(new Edge(v, w));
        if (!directed && u != v) {
            adj.get(v).add(new Edge(u, w));
        }
        edgeCount++;
    }

    public int vertexCount() { return n; }

    public int edgeCount() { return edgeCount; }

    public boolean isDirected() { return directed; }

    public List<Edge> neighbors(int u) {
        checkVertex(u);
        return Collections.unmodifiableList(adj.get(u));
    }

    public int outDegree(int u) {
        checkVertex(u);
        return adj.get(u).size();
    }

    public Edge edgeAt(int u, int i) {
        return adj.get(u).get(i);
    }

    public boolean hasEdge(int u, int v) {
        checkVertex(u);
        checkVertex(v);
        for (Edge e : adj.get(u)) {
            if (e.to() == v) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSelfLoop(int u) {
        return hasEdge(u, u);
    }

    public Graph reversed() {
        Graph r = new Graph(n, directed);
        for (int u = 0; u < n; u++) {
            for (Edge e : adj.get(u)) {
                r.adj.get(e.to()).add(new Edge(u, e.weight()));
            }
        }
        r.edgeCount = edgeCount;
        return r;
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= n) {
            throw new IllegalArgumentException("Vertex " + v + " out of range 0.." + (n - 1));
        }
    }

    public static Graph fromFile(Path path) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = nextLine(in);
            if (line == null) {
                throw new IOException("Empty file: " + path);
            }
            String[] h = line.split("\\s+");
            if (h.length != 3) {
                throw new IOException("Header must be 'n m directed|undirected': " + line);
            }
            int n;
            int m;
            try {
                n = Integer.parseInt(h[0]);
                m = Integer.parseInt(h[1]);
            } catch (NumberFormatException ex) {
                throw new IOException("Bad header numbers: " + line, ex);
            }
            if (n < 0 || m < 0) {
                throw new IOException("n and m must be >= 0: " + line);
            }
            boolean directed;
            switch (h[2].toLowerCase()) {
                case "directed" -> directed = true;
                case "undirected" -> directed = false;
                default -> throw new IOException("Expected directed|undirected, got: " + h[2]);
            }

            Graph g = new Graph(n, directed);
            for (int i = 0; i < m; i++) {
                line = nextLine(in);
                if (line == null) {
                    throw new IOException("Expected " + m + " edges, found only " + i);
                }
                String[] p = line.split("\\s+");
                if (p.length != 3) {
                    throw new IOException("Edge line must be 'u v w': " + line);
                }
                try {
                    g.addEdge(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Bad edge line '" + line + "': " + ex.getMessage(), ex);
                }
            }
            return g;
        }
    }

    private static String nextLine(BufferedReader in) throws IOException {
        String s;
        while ((s = in.readLine()) != null) {
            s = s.trim();
            if (!s.isEmpty() && !s.startsWith("#")) {
                return s;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(n).append(' ').append(edgeCount).append(' ')
          .append(directed ? "directed" : "undirected").append('\n');
        for (int u = 0; u < n; u++) {
            sb.append(u).append(":");
            for (Edge e : adj.get(u)) {
                sb.append(" (").append(e.to()).append(',').append(e.weight()).append(')');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
