package bench;

import graph.Graph;
import java.io.IOException;
import java.nio.file.Path;
import paths.DagShortestPath;
import paths.PathResult;

public final class DagPathsDemo {

    private DagPathsDemo() { }

    public static void main(String[] args) throws IOException {
        Graph g = Graph.fromFile(Path.of(args.length > 0 ? args[0] : "data/jobs_dag.txt"));
        PathResult r = DagShortestPath.fromAllSources(g);
        System.out.println("sources (in-degree 0, distance 0): " + r.sources());
        for (int v = 0; v < g.vertexCount(); v++) {
            System.out.println("  " + v + ": " + r.formatPath(v));
        }
        System.out.println("sum of distances = " + r.sumOfDistances() + "   work: " + r.metrics());
    }
}
