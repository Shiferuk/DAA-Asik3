package dsu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DsuReferenceTest {

    private static int distinct(int[] label) {
        Set<Integer> s = new HashSet<>();
        for (int x : label) {
            s.add(x);
        }
        return s.size();
    }

    @Test
    void findAndComponentsAgreeWithReferenceAfterRandomUnions() {
        Random rnd = new Random(1);
        for (int iter = 0; iter < 300; iter++) {
            int n = 1 + rnd.nextInt(80);
            DisjointSetUnion d = new DisjointSetUnion(n);
            int[] label = new int[n];
            for (int i = 0; i < n; i++) {
                label[i] = i;
            }
            int ops = rnd.nextInt(3 * n + 1);
            for (int op = 0; op < ops; op++) {
                int x = rnd.nextInt(n);
                int y = rnd.nextInt(n);
                boolean refMerges = label[x] != label[y];
                if (refMerges) {
                    int from = label[y];
                    for (int i = 0; i < n; i++) {
                        if (label[i] == from) {
                            label[i] = label[x];
                        }
                    }
                }
                assertEquals(refMerges, d.union(x, y));                                                             
                assertEquals(distinct(label), d.components());                                                           
                int a = rnd.nextInt(n);
                int b = rnd.nextInt(n);
                assertEquals(label[a] == label[b], d.find(a) == d.find(b));
            }
            for (int a = 0; a < n; a++) {                                                          
                for (int b = 0; b < n; b++) {
                    assertEquals(label[a] == label[b], d.find(a) == d.find(b));
                }
            }
            assertEquals(distinct(label), d.components());
            assertEquals(n - distinct(label), d.successfulUnionCalls());
        }
    }

    @Test
    void setSizesAgreeWithReference() {
        Random rnd = new Random(2);
        int n = 200;
        DisjointSetUnion d = new DisjointSetUnion(n);
        int[] label = new int[n];
        for (int i = 0; i < n; i++) {
            label[i] = i;
        }
        for (int op = 0; op < 150; op++) {
            int x = rnd.nextInt(n);
            int y = rnd.nextInt(n);
            if (label[x] != label[y]) {
                int from = label[y];
                for (int i = 0; i < n; i++) {
                    if (label[i] == from) {
                        label[i] = label[x];
                    }
                }
            }
            d.union(x, y);
        }
        for (int a = 0; a < n; a++) {
            int size = 0;
            for (int b = 0; b < n; b++) {
                if (label[a] == label[b]) {
                    size++;
                }
            }
            assertEquals(size, d.setSize(a));
        }
    }
}
