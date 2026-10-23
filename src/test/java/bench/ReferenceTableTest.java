package bench;

import static org.junit.jupiter.api.Assertions.*;

import graph.Graph;
import metrics.Counter;
import mst.Mst;
import mst.MstResult;
import order.TopoResult;
import order.TopologicalSort;
import org.junit.jupiter.api.Test;
import paths.DagShortestPath;
import paths.PathResult;
import scc.Kosaraju;
import scc.SccResult;

class ReferenceTableTest {

    @Test
    void tableHasTenRowsInFiveSizesAndTwoDensities() {
        assertEquals(10, ReferenceTable.ROWS.length);
        for (ReferenceTable.Row r : ReferenceTable.ROWS) {
            assertTrue(r.m() == 4L * r.n() || r.m() == 20L * r.n());
        }
    }

    @Test
    void everyRowMatchesToTheLastDigit() {
        for (ReferenceTable.Row ref : ReferenceTable.ROWS) {
            String name = ref.n() + "/" + ref.m();

            Graph und = RefGraphs.undirected(ref.n(), ref.m());
            MstResult k = Mst.kruskal(und);
            MstResult p = Mst.prim(und);
            assertEquals(ref.mstWeight(), k.totalWeight(), name + " Kruskal");
            assertEquals(ref.mstWeight(), p.totalWeight(), name + " Prim");
            assertEquals(ref.n() - 1, k.edges().size());
            assertEquals(ref.n() - 1, p.edges().size());

            Graph dir = RefGraphs.directed(ref.n(), ref.m());
            SccResult s = Kosaraju.run(dir);
            assertEquals(ref.sccCount(), s.count(), name + " SCC");
            assertEquals(2L * ref.m(), s.metrics().get(Counter.EDGES_SCANNED));                  

            Graph dag = RefGraphs.dag(ref.n(), ref.m());
            TopoResult t = TopologicalSort.kahn(dag);
            assertEquals(ref.n(), t.processedCount());
            assertEquals(ref.m(), t.metrics().get(Counter.EDGES_SCANNED));                         
            PathResult d = DagShortestPath.fromAllSources(dag, t.order());
            assertEquals(ref.dagDistanceSum(), d.sumOfDistances(), name + " DAG distances");
            assertEquals(ref.m(), d.metrics().get(Counter.EDGES_SCANNED));                               
        }
    }
}
