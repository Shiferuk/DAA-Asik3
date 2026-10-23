package bench;

public final class ReferenceTable {

    public record Row(int n, int m, long mstWeight, int sccCount, long dagDistanceSum) { }

    public static final Row[] ROWS = {
        new Row(1_000, 4_000, 147_970L, 27, 396_274L),
        new Row(1_000, 20_000, 30_363L, 1, 252_411L),
        new Row(3_162, 12_648, 468_911L, 137, 1_271_911L),
        new Row(3_162, 63_240, 97_589L, 1, 723_157L),
        new Row(10_000, 40_000, 1_486_139L, 373, 3_941_828L),
        new Row(10_000, 200_000, 309_550L, 1, 2_381_342L),
        new Row(31_623, 126_492, 4_715_515L, 1_188, 12_551_891L),
        new Row(31_623, 632_460, 968_517L, 1, 7_376_612L),
        new Row(100_000, 400_000, 15_012_094L, 3_923, 40_096_539L),
        new Row(100_000, 2_000_000, 3_056_681L, 1, 23_512_087L),
    };

    private ReferenceTable() { }

    public static Row find(int n, int m) {
        for (Row r : ROWS) {
            if (r.n() == n && r.m() == m) {
                return r;
            }
        }
        return null;
    }
}
