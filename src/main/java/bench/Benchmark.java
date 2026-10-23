package bench;

import graph.Graph;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import metrics.Metrics;
import mst.Mst;
import mst.MstResult;
import order.TopoResult;
import order.TopologicalSort;
import paths.DagShortestPath;
import paths.PathResult;
import scc.Kosaraju;
import scc.SccResult;

public final class Benchmark {

    public static final int[] SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    public static final int[] DENSITY_FACTORS = {4, 20};                                               
    public static final String HEADER =
            "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";

    private static final int TIMED_RUNS = 5;

    public record Row(String algorithm, String graph, int n, int m, double timeMs,
                      long dsuOps, long edgeScans, long comparisons, long maxDepth, long result) {

        String toCsv() {
            return String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d",
                    algorithm, graph, n, m, timeMs, dsuOps, edgeScans, comparisons, maxDepth, result);
        }
    }

    private Benchmark() { }

    public static void main(String[] args) {
        Path csv = Path.of(args.length > 0 ? args[0] : "results.csv");
        try {
            List<Row> rows = run(SIZES, csv, System.out);
            System.out.println("Wrote " + rows.size() + " rows to " + csv.toAbsolutePath());
            System.out.println("ALL OK: all cross-checks passed.");
        } catch (IllegalStateException | IOException ex) {
            System.err.println("BENCHMARK FAILED: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static List<Row> run(int[] sizes, Path csv, PrintStream log) throws IOException {
        List<Row> rows = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        log.printf("%-12s %-14s %8s %9s %9s %11s %11s %12s %9s %14s%n",
                "algorithm", "graph", "n", "m", "time_ms", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result");
        for (int n : sizes) {
            for (int factor : DENSITY_FACTORS) {
                int m = factor * n;
                String name = "n" + n + "_" + (factor == 4 ? "sparse" : "dense");

                Graph und = RefGraphs.undirected(n, m);
                Timed<MstResult> kr = measure("Kruskal " + name, () -> Mst.kruskal(und), MstResult::metrics);
                Timed<MstResult> pr = measure("Prim " + name, () -> Mst.prim(und), MstResult::metrics);
                Verify.mstAgreement(name, und, kr.value, pr.value);
                rows.add(row("Kruskal", name, und, kr, kr.value.totalWeight(), log));
                rows.add(row("Prim", name, und, pr, pr.value.totalWeight(), log));

                Graph dir = RefGraphs.directed(n, m);
                Timed<SccResult> sc = measure("Kosaraju " + name, () -> Kosaraju.run(dir), SccResult::metrics);
                Verify.sccAndTopo(name + " (directed)", dir, sc.value, TopologicalSort.kahn(dir));
                rows.add(row("Kosaraju", name, dir, sc, sc.value.count(), log));

                Graph dag = RefGraphs.dag(n, m);
                Timed<TopoResult> ka = measure("Kahn " + name, () -> TopologicalSort.kahn(dag), TopoResult::metrics);
                Verify.sccAndTopo(name + " (DAG)", dag, Kosaraju.run(dag), ka.value);
                int[] order = ka.value.order();
                Timed<PathResult> ds = measure("DagShortestPath " + name,
                        () -> DagShortestPath.fromAllSources(dag, order), PathResult::metrics);
                Verify.dagDistances(name, dag, ds.value);
                rows.add(row("Kahn", name, dag, ka, ka.value.processedCount(), log));
                rows.add(row("DagShortestPath", name, dag, ds, ds.value.sumOfDistances(), log));

                Verify.scanLaws(name, m, sc.metrics.get(metrics.Counter.EDGES_SCANNED),
                        ka.metrics.get(metrics.Counter.EDGES_SCANNED), ds.metrics.get(metrics.Counter.EDGES_SCANNED));
                ReferenceTable.Row ref = ReferenceTable.find(n, m);
                if (ref != null) {
                    Verify.referenceRow(name, ref, kr.value.totalWeight(), pr.value.totalWeight(),
                            sc.value.count(), ds.value.sumOfDistances());
                }
                summary.add(String.format(Locale.ROOT, "%,10d / %,-10d %,16d %,12d %,20d %s",
                        n, m, kr.value.totalWeight(), sc.value.count(), ds.value.sumOfDistances(),
                        ref != null ? "matches reference" : "(no reference row)"));
            }
        }
        log.println();
        log.printf("%-23s %16s %12s %20s%n", "Graph (n / m)", "MST total weight", "SCC count", "Sum of DAG distances");
        summary.forEach(log::println);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Row r : rows) {
            lines.add(r.toCsv());
        }
        Files.write(csv, lines);
        return rows;
    }

    private static <T> Row row(String algo, String name, Graph g, Timed<T> t, long result, PrintStream log) {
        Metrics m = t.metrics;
        Row r = new Row(algo, name, g.vertexCount(), g.edgeCount(), t.medianMs, m.dsuOps(),
                m.get(metrics.Counter.EDGES_SCANNED), m.get(metrics.Counter.COMPARISONS),
                m.get(metrics.Counter.MAX_STACK_DEPTH), result);
        log.printf(Locale.ROOT, "%-12s %-14s %8d %9d %9.2f %11d %11d %12d %9d %14d%n",
                r.algorithm(), r.graph(), r.n(), r.m(), r.timeMs(), r.dsuOps(), r.edgeScans(),
                r.comparisons(), r.maxDepth(), r.result());
        return r;
    }

    private record Timed<T>(T value, Metrics metrics, double medianMs) { }

    private static <T> Timed<T> measure(String label, Supplier<T> algo, Function<T, Metrics> metricsOf) {
        T last = algo.get();                                            
        String expected = metricsOf.apply(last).toString();
        long[] times = new long[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long t0 = System.nanoTime();
            last = algo.get();
            times[i] = System.nanoTime() - t0;
            String got = metricsOf.apply(last).toString();
            if (!got.equals(expected)) {
                throw new IllegalStateException(label + ": counters differ between runs: [" + expected + "] vs [" + got + "]");
            }
        }
        Arrays.sort(times);
        return new Timed<>(last, metricsOf.apply(last), times[TIMED_RUNS / 2] / 1e6);
    }
}
