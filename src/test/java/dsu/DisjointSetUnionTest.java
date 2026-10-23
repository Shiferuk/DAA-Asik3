package dsu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DisjointSetUnionTest {

    @Test
    void makeSetCreatesSingletons() {
        DisjointSetUnion d = new DisjointSetUnion(5);
        assertEquals(5, d.components());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, d.find(i));
            assertEquals(1, d.setSize(i));
        }
    }

    @Test
    void makeSetResetsStateAndCounters() {
        DisjointSetUnion d = new DisjointSetUnion(4);
        d.union(0, 1);
        d.makeSet(6);
        assertEquals(6, d.components());
        assertEquals(6, d.elementCount());
        assertEquals(0, d.findCalls());
        assertEquals(0, d.successfulUnionCalls());
        assertNotEquals(d.find(0), d.find(1));
    }

    @Test
    void zeroElementsIsFine() {
        DisjointSetUnion d = new DisjointSetUnion(0);
        assertEquals(0, d.components());
        assertThrows(IllegalArgumentException.class, () -> d.find(0));
        assertThrows(IllegalArgumentException.class, () -> d.makeSet(-1));
    }

    @Test
    void unionMergesAndReportsResult() {
        DisjointSetUnion d = new DisjointSetUnion(6);
        assertTrue(d.union(0, 1));
        assertTrue(d.union(2, 3));
        assertTrue(d.union(1, 3));
        assertFalse(d.union(0, 2));                      
        assertFalse(d.union(4, 4));               
        assertEquals(d.find(0), d.find(3));
        assertNotEquals(d.find(0), d.find(4));
        assertEquals(3, d.components());                     
        assertEquals(4, d.setSize(2));
    }

    @Test
    void outOfRangeRejected() {
        DisjointSetUnion d = new DisjointSetUnion(3);
        assertThrows(IllegalArgumentException.class, () -> d.find(3));
        assertThrows(IllegalArgumentException.class, () -> d.find(-1));
        assertThrows(IllegalArgumentException.class, () -> d.union(0, 9));
    }

    @Test
    void unionBySizeKeepsLargerRootRegardlessOfArgumentOrder() {
        DisjointSetUnion d = new DisjointSetUnion(5);
        d.union(0, 1);
        d.union(0, 2);                                         
        int big = d.find(0);
        d.union(3, 0);                                               
        assertEquals(big, d.find(3));
        d.union(big, 4);                                             
        assertEquals(big, d.find(4));
        assertEquals(0, d.depth(big));
        assertEquals(1, d.depth(3));                                             
    }

    @Test
    void pathCompressionFlattensTree() {
        DisjointSetUnion d = new DisjointSetUnion(4);
        d.union(0, 1);
        d.union(2, 3);
        d.union(0, 2);                                                      
        assertEquals(2, d.depth(3));                 
        assertEquals(0, d.find(3));
        assertEquals(1, d.depth(3));                        
        assertEquals(1, d.depth(2));
    }

    @Test
    void countersCountFindsAndOnlySuccessfulUnions() {
        DisjointSetUnion d = new DisjointSetUnion(4);
        d.find(0);
        d.find(1);
        assertEquals(2, d.findCalls());
        assertEquals(0, d.successfulUnionCalls());

        d.union(0, 1);                                             
        assertEquals(4, d.findCalls());
        assertEquals(1, d.successfulUnionCalls());

        d.union(0, 1);                                                       
        assertEquals(6, d.findCalls());
        assertEquals(1, d.successfulUnionCalls());

        d.union(2, 3);
        assertEquals(2, d.successfulUnionCalls());
        assertEquals(2, d.components());                                  

        d.resetCounters();
        assertEquals(0, d.findCalls());
        assertEquals(0, d.successfulUnionCalls());
        assertEquals(2, d.components());                                
    }

    @Test
    void invalidCallsDoNotTouchCounters() {
        DisjointSetUnion d = new DisjointSetUnion(2);
        assertThrows(IllegalArgumentException.class, () -> d.find(5));
        assertEquals(0, d.findCalls());
    }

    @Test
    void largeChainOfUnionsNoStackOverflowAndShallowTrees() {
        int n = 200_000;
        DisjointSetUnion d = new DisjointSetUnion(n);
        for (int i = 0; i + 1 < n; i++) {
            assertTrue(d.union(i, i + 1));
        }
        assertEquals(1, d.components());
        assertEquals(n - 1, d.successfulUnionCalls());
        int root = d.find(0);
        for (int i = 0; i < n; i += 997) {
            assertEquals(root, d.find(i));
        }
        assertEquals(n, d.setSize(n / 2));
    }

    @Test
    void worstCaseBinaryMergingStaysLogarithmic() {

        int n = 1 << 12;
        DisjointSetUnion d = new DisjointSetUnion(n);
        for (int step = 1; step < n; step *= 2) {
            for (int i = 0; i + step < n; i += 2 * step) {
                d.union(i, i + step);
            }
        }
        int maxDepth = 0;
        for (int i = 0; i < n; i++) {
            maxDepth = Math.max(maxDepth, d.depth(i));
        }
        assertTrue(maxDepth <= 12);
        assertEquals(1, d.components());
    }
}
