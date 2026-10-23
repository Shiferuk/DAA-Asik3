# citygraph

City platform graph toolkit, written from scratch (the Java standard library has no graph types).
Java 17+, Maven, JUnit 5. No third-party code.

| Question | Answer from | Package |
|---|---|---|
| Which roads to build so every district is reachable at the lowest total cost? | Kruskal / Prim (minimum spanning forest) | `mst` |
| In which order may dependent jobs run? | Kahn topological order | `order` |
| Which jobs form a deadlock? | Kosaraju strongly connected components | `scc` |
| Fastest route through the pipeline? | DAG shortest path with a virtual super-source (Dijkstra on general graphs) | `paths` |

## Run

    mvn test
    mvn -q compile && java -Xmx2g -cp target/classes bench.Benchmark results.csv   # graded benchmark, 50 rows
    java -cp target/classes bench.DagPathsDemo data/jobs_dag.txt                  # prints restored paths
    java -cp target/classes bench.DataDemo data                                   # counter tables on data/ files

`bench.Benchmark` runs 5 sizes x 2 densities x (undirected, directed, DAG) with the exact generator
`bench.RefGraphs`, writes `results.csv` and checks automatically that Kruskal/Prim agree, Kosaraju/Kahn agree,
the DAG distances are optimal, the counters are identical on every run, and the results equal the reference table
(`bench.ReferenceTable`) to the last digit. On any disagreement it prints
`BENCHMARK FAILED: ...` and exits with status 1. **See `REPORT.md`** for the explanation of negative weights,
the counter definitions and the conclusions drawn from the counters.

## Input format (`data/*.txt`)

First line `n m directed|undirected`, then `m` lines `u v w`. Blank lines and `#` comments are ignored.
Vertices are `0..n-1`, weights are `int`. Bad files throw `IOException` with the offending line.

| File | Shape |
|---|---|
| `roads_connected.txt` | 6 districts, 9 candidate roads, connected (MST weight 33) |
| `roads_disconnected.txt` | 2 cut-off districts, a self-loop, parallel roads: forest with 4 components |
| `jobs_dag.txt` | acyclic pipeline with a diamond |
| `jobs_deadlock.txt` | cycles 0->1->2->0 and 3<->4, a self-wait at 5, job 6 waiting on the cycle, independent job 7 |
| `jobs_independent.txt` | 4 jobs, no edges |

## Design

* `graph.Graph`: one adjacency-list class for directed and undirected graphs. Parallel edges are kept; an
  undirected self-loop is stored once; `reversed()` flips every arc.
* `metrics.Metrics` / `Counter`: every algorithm counts its own work (edges scanned, vertices visited,
  comparisons, find calls, successful unions, priority-queue pushes/pops/stale pops, relaxations, stack
  pushes, **maximum stack depth**, maximum queue size). Each result object carries its `Metrics`.
* `util.IntStack` / `IntQueue`: own explicit stack and queue that remember their peak size.
* `dsu.DisjointSetUnion`: union by size + path compression (both), counts `find` calls (union adds 2) and
  successful unions.
* `scc.Kosaraju`: pass 1 iterative DFS for finish order, `Graph.reversed()`, pass 2 in decreasing finish
  order; each tree is one component. Ids come out in topological order of the condensation.
  `SccResult.deadlockGroups(g)` = components of size > 1, or a single job with a self-loop.
* `order.TopologicalSort.kahn`: in-degrees + queue. If fewer than n vertices leave the queue the result
  says `hasCycle()` and lists the stuck vertices; `order()` then **throws** instead of returning a wrong
  order. `isValidOrder(g, order)` checks a permutation and `pos[u] < pos[v]` for every edge (a self-loop
  always fails). Undirected input is rejected with `IllegalArgumentException`.
* Edge cases (self-loops, parallel edges, isolated vertices, disconnected graphs, n = 0) never crash.
  MST treats direction as irrelevant and returns a forest plus the component count.

## Which algorithm for which situation

Counts below are from `bench.DataDemo` (seed 42, deterministic); times vary by machine.

**Roads (spanning tree).** The road department's input is a list of candidate roads, so Kruskal is the
natural fit: sort once, then DSU decides. On the random 10k/50k graph Kruskal did 716 227 weight comparisons
and 119 986 find calls; our Prim (lazy priority queue, no decrease-key) did 1 458 654 comparisons and 49 994
pushes, and held up to 38 282 edges in the queue. On the dense 2k/200k graph Prim pushed 199 896 edges and made
6.5M comparisons against Kruskal's 3.3M. On the ten benchmark graphs Prim needs about twice Kruskal's
comparisons every time (see `REPORT.md`). Prim's strength (grow from one vertex, no global sort) shows when the
graph is already an adjacency structure and you only need the tree around one start vertex, or with an indexed
heap, which this project does not implement. Both give the same total weight on every graph (checked
automatically).

**Job order.** Use Kahn. It is O(V + E), and the same pass tells you whether an order exists. On the
cyclic random graph (100k/300k) it examined only 18 051 edges before the queue ran dry (a cycle blocks it),
while Kosaraju always scans 2m = 600 000.

**Deadlock.** Kahn only says "cycle"; Kosaraju says *which* jobs. Run Kahn first, and Kosaraju only when it
reports a cycle, to name the deadlock groups. The two are cross-checked: Kahn's stuck set must equal
everything reachable from a deadlock group.

**Fastest route.** For roads (general graph, non-negative costs) use Dijkstra. For the job pipeline, which
is a DAG, relax edges in topological order (`DagShortestPath`): every in-degree-0 job starts at 0 (virtual
super-source), `parent[]` restores the path, no priority queue, O(V + E), negative weights allowed. It refuses
cyclic graphs.

**Why iterative DFS.** On a 100 000-vertex path the explicit stack reaches depth 100 000
(`MAX_STACK_DEPTH`), and plain recursion to that depth throws `StackOverflowError` on a default JVM
(checked). Tests run Kosaraju on a 100 000 path and on a 100 000 cycle with default stack settings.

## Feature implementation notes

- **DSU (`feature/dsu`)**: union by size and path compression are both enabled; DSU operation counters are exposed through each algorithm's metrics.
- **MST (`feature/mst`)**: Kruskal uses a deterministic merge sort and DSU; Prim uses a lazy priority queue. Both return a forest for disconnected inputs.
- **SCC and ordering (`feature/scc`)**: Kosaraju uses explicit stacks; Kahn detects cycles and does not expose an invalid partial order as a valid result.
- **DAG paths (`feature/paths`)**: topological relaxation supports negative edge weights, initializes all sources to distance zero, and restores paths with parent pointers.
- **Metrics and report (`feature/metrics`)**: counters are collected in the algorithms; `REPORT.md` explains their formulas and includes three plots under `results/plots/`.

## Git workflow

The repository uses `main` for the integrated release and feature branches for focused work. The expected branch names are `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, and `feature/metrics`. Use small commits with messages such as `feat(dsu): add path compression`, `test(mst): compare kruskal and prim weights`, and `docs(report): add plots`. After merging and running tests/benchmark, tag the release as `v1.0`.

Typical commands:

```bash
git switch -c feature/dsu
# make and test one focused change
git add src/test/java/dsu README.md
git commit -m "test(dsu): verify reference components"
git switch main
git merge --no-ff feature/dsu
# repeat for the other feature branches
mvn test
mvn -q compile && java -Xmx2g -cp target/classes bench.Benchmark results.csv
git tag -a v1.0 -m "Assignment 3 release"
```

The ZIP supplied for review did not include a `.git` directory, so any Git history created from this snapshot starts at the point of review rather than recovering earlier development history.

- DSU operations use union by size and path compression.

- Both MST implementations return a forest when the input is disconnected.

- Kosaraju is iterative and Kahn explicitly reports cyclic input.

- DAG shortest paths support negative weights and restore paths.

- Counter ratios and scaling slopes are documented with plots.
