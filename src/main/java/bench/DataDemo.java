package bench;

import graph.Graph;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import metrics.Counter;
import metrics.Metrics;
import mst.Mst;
import mst.MstResult;
import order.TopoResult;
import order.TopologicalSort;
import scc.Kosaraju;
import scc.SccResult;

public final class DataDemo {

    private static final int WARMUP = 2;
    private static final int RUNS = 5;

    private DataDemo() { }

    public static void main(String[] args) {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        try {
            List<Path> files = dataFiles(dataDir);
            Random rnd = new Random(42);

            System.out.println("== Road planning: Kruskal vs Prim (work counters + median ms) ==");
            System.out.printf("%-26s %6s %7s %4s %10s | %8s %8s %8s %8s | %8s %8s %8s %8s %8s%n",
                    "graph", "n", "m", "comp", "weight",
                    "K.ms", "K.cmp", "K.finds", "K.unions",
                    "P.ms", "P.cmp", "P.push", "P.pop", "P.maxPQ");
            for (Path f : files) {
                mst(f.getFileName().toString(), Graph.fromFile(f));
            }
            mst("random sparse 1k/3k", Generators.randomUndirected(rnd, 1_000, 3_000));
            mst("random 10k/50k", Generators.randomUndirected(rnd, 10_000, 50_000));
            mst("random 50k/200k", Generators.randomUndirected(rnd, 50_000, 200_000));
            mst("random dense 2k/200k", Generators.randomUndirected(rnd, 2_000, 200_000));
            mst("random disconnected 20k/10k", Generators.randomUndirected(rnd, 20_000, 10_000));

            System.out.println();
            System.out.println("== Scheduling: Kosaraju SCC vs Kahn (work counters + median ms) ==");
            System.out.printf("%-26s %7s %7s | %6s %5s %6s | %8s %8s %8s | %8s %8s %8s%n",
                    "graph", "n", "m", "SCCs", "lock", "acycl",
                    "S.ms", "S.edges", "S.maxStk", "K.ms", "K.edges", "K.maxQ");
            for (Path f : files) {
                Graph g = Graph.fromFile(f);
                if (g.isDirected()) {
                    sccTopo(f.getFileName().toString(), g);
                }
            }
            sccTopo("random DAG 100k/300k", Generators.randomDag(rnd, 100_000, 300_000));
            sccTopo("random directed 100k/300k", Generators.randomDirected(rnd, 100_000, 300_000));
            sccTopo("random directed 100k/80k", Generators.randomDirected(rnd, 100_000, 80_000));
            sccTopo("chain 100k", Generators.chain(100_000, false));
            sccTopo("chain+back edge 100k", Generators.chain(100_000, true));
            sccTopo("star 100k", Generators.star(100_000));

            System.out.println();
            System.out.println("ALL OK: Kruskal/Prim agree and Kosaraju/Kahn agree on every graph.");
        } catch (IllegalStateException | IOException ex) {
            System.err.println("BENCHMARK FAILED: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void mst(String name, Graph g) {
        MstResult k = null;
        MstResult p = null;
        for (int i = 0; i < WARMUP; i++) {
            k = Mst.kruskal(g);
            p = Mst.prim(g);
        }
        long[] kt = new long[RUNS];
        long[] pt = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            k = Mst.kruskal(g);
            long t1 = System.nanoTime();
            p = Mst.prim(g);
            long t2 = System.nanoTime();
            kt[i] = t1 - t0;
            pt[i] = t2 - t1;
        }
        Verify.mstAgreement(name, g, k, p);
        Metrics km = k.metrics();
        Metrics pm = p.metrics();
        System.out.printf("%-26s %6d %7d %4d %10d | %8.2f %8d %8d %8d | %8.2f %8d %8d %8d %8d%n",
                name, g.vertexCount(), g.edgeCount(), k.components(), k.totalWeight(),
                median(kt) / 1e6, km.get(Counter.COMPARISONS), km.get(Counter.FIND_CALLS),
                km.get(Counter.SUCCESSFUL_UNIONS),
                median(pt) / 1e6, pm.get(Counter.COMPARISONS), pm.get(Counter.PQ_PUSHES),
                pm.get(Counter.PQ_POPS), pm.get(Counter.MAX_QUEUE_SIZE));
    }

    private static void sccTopo(String name, Graph g) {
        SccResult s = null;
        TopoResult t = null;
        for (int i = 0; i < WARMUP; i++) {
            s = Kosaraju.run(g);
            t = TopologicalSort.kahn(g);
        }
        long[] st = new long[RUNS];
        long[] tt = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            s = Kosaraju.run(g);
            long t1 = System.nanoTime();
            t = TopologicalSort.kahn(g);
            long t2 = System.nanoTime();
            st[i] = t1 - t0;
            tt[i] = t2 - t1;
        }
        Verify.sccAndTopo(name, g, s, t);
        System.out.printf("%-26s %7d %7d | %6d %5d %6s | %8.2f %8d %8d | %8.2f %8d %8d%n",
                name, g.vertexCount(), g.edgeCount(), s.count(), s.deadlockGroups(g).size(),
                t.isAcyclic() ? "yes" : "NO",
                median(st) / 1e6, s.metrics().get(Counter.EDGES_SCANNED),
                s.metrics().get(Counter.MAX_STACK_DEPTH),
                median(tt) / 1e6, t.metrics().get(Counter.EDGES_SCANNED),
                t.metrics().get(Counter.MAX_QUEUE_SIZE));
    }

    private static List<Path> dataFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            ds.forEach(files::add);
        }
        files.sort(null);
        if (files.isEmpty()) {
            throw new IllegalStateException("no .txt graphs found in " + dir.toAbsolutePath());
        }
        return files;
    }

    private static long median(long[] a) {
        long[] c = a.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }
}
