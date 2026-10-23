package mst;

import static org.junit.jupiter.api.Assertions.*;

import bench.Generators;
import bench.Verify;
import graph.Graph;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MstConnectedTest {

    @Test
    void kruskalAndPrimAgreeOnHundredRandomConnectedGraphs() {
        Random rnd = new Random(100);
        for (int i = 0; i < 100; i++) {
            int n = 1 + rnd.nextInt(60);
            int m = (n - 1) + rnd.nextInt(4 * n + 1);
            Graph g = Generators.randomConnected(rnd, n, m);

            MstResult k = Mst.kruskal(g);
            MstResult p = Mst.prim(g);

            assertEquals(k.totalWeight(), p.totalWeight());
            assertEquals(n - 1, k.edges().size());
            assertEquals(n - 1, p.edges().size());
            assertEquals(1, k.components());
            assertEquals(1, p.components());
            Verify.mstAgreement("connected#" + i, g, k, p);                                                     
        }
    }
}
