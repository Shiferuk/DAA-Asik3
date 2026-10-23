package bench;

import static org.junit.jupiter.api.Assertions.*;

import graph.Graph;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import metrics.Counter;
import mst.Mst;
import mst.MstResult;
import order.TopoResult;
import order.TopologicalSort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paths.DagShortestPath;
import paths.PathResult;
import scc.Kosaraju;
import scc.SccResult;

class BenchmarkTest {

    private static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());

    @Test
    void csvHasHeaderAndOneRowPerAlgorithmAndGraph(@TempDir Path tmp) throws IOException {
        Path csv = tmp.resolve("results.csv");
        List<Benchmark.Row> rows = Benchmark.run(new int[] {1000}, csv, SILENT);
        assertEquals(10, rows.size());                                                       
        List<String> lines = Files.readAllLines(csv);
        assertEquals(11, lines.size());
        assertEquals("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result", lines.get(0));
        for (String l : lines.subList(1, lines.size())) {
            assertEquals(10, l.split(",").length);
        }
        assertTrue(lines.get(1).startsWith("Kruskal,n1000_sparse,1000,4000,"));
    }

    @Test
    void countersAreIdenticalOnEveryRun(@TempDir Path tmp) throws IOException {
        List<Benchmark.Row> a = Benchmark.run(new int[] {1000}, tmp.resolve("a.csv"), SILENT);
        List<Benchmark.Row> b = Benchmark.run(new int[] {1000}, tmp.resolve("b.csv"), SILENT);
        for (int i = 0; i < a.size(); i++) {
            Benchmark.Row x = a.get(i);
            Benchmark.Row y = b.get(i);
            assertEquals(x.dsuOps(), y.dsuOps());
            assertEquals(x.edgeScans(), y.edgeScans());
            assertEquals(x.comparisons(), y.comparisons());
            assertEquals(x.maxDepth(), y.maxDepth());
            assertEquals(x.result(), y.result());
        }
    }

    @Test
    void counterLaws() {
        int n = 1000;
        int m = 4000;
        Graph und = RefGraphs.undirected(n, m);
        MstResult k = Mst.kruskal(und);
        MstResult p = Mst.prim(und);
        assertEquals(k.totalWeight(), p.totalWeight());
        assertEquals(2L * m + 3L * (n - 1), k.metrics().dsuOps());                                                          
        assertEquals(m, k.metrics().get(Counter.EDGES_SCANNED));
        assertTrue(k.metrics().get(Counter.COMPARISONS) <= (long) m * 12);                                     
        assertEquals(0, p.metrics().dsuOps());
        assertEquals(2L * m, p.metrics().get(Counter.EDGES_SCANNED));                          

        Graph dir = RefGraphs.directed(n, m);
        SccResult s = Kosaraju.run(dir);
        assertEquals(2L * m, s.metrics().get(Counter.EDGES_SCANNED));                                       
        assertTrue(s.metrics().get(Counter.MAX_STACK_DEPTH) >= 1);
        assertEquals(0, s.metrics().get(Counter.COMPARISONS));

        Graph dag = RefGraphs.dag(n, m);
        TopoResult t = TopologicalSort.kahn(dag);
        assertEquals(n, t.processedCount());
        assertEquals(m, t.metrics().get(Counter.EDGES_SCANNED));                                                       
        PathResult d = DagShortestPath.fromAllSources(dag, t.order());
        assertEquals(m, d.metrics().get(Counter.EDGES_SCANNED));
        assertEquals(m, d.metrics().get(Counter.COMPARISONS));
    }
}
