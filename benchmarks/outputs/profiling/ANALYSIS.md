# Parser Profiling Analysis

**Date:** 2026-02-28
**Benchmark:** ParserProfilingBenchmark.pureParseOnly (pre-tokenized input, parser-only)
**JMH:** 1.37, JDK 21.0.10 (OpenJDK), Fork 1, 5 warmup / 5 measurement iterations
**Hardware:** Apple M-series (macOS, aarch64)

## GC Profiling Results (-prof gc)

### recursive_json (size=2000)

| Metric | Value |
|--------|-------|
| Parse time (avg) | 20.5 ms/op |
| Allocation rate | 850 MB/sec |
| Allocation per op | **17.5 MB/op** (17,467,352 B) |
| GC count (total) | 43 |
| GC time (total) | 580 ms |

### iterative_json (size=2000)

| Metric | Value |
|--------|-------|
| Parse time (avg) | 22.3 ms/op |
| Allocation rate | 2,706 MB/sec |
| Allocation per op | **63.1 MB/op** (63,141,477 B) |
| GC count (total) | 149 |
| GC time (total) | 321 ms |

**Key observation:** iterative_json allocates 3.6x more per operation than recursive_json.
This correlates with iterative_json having more reductions (flat list of key-value pairs
vs deeply nested structure), meaning more `stack.take(k).map(_.node).reverse` chains.

## Stack Profiling Results (-prof stack)

Both recursive_json and iterative_json show identical thread state distributions:

| Thread State | Percentage | Dominant Method |
|-------------|-----------|-----------------|
| WAITING | 78.5% | `Unsafe.park` (71.4%), `Continuation.run` (9.0%) |
| TIMED_WAITING | 14.3% | `Unsafe.park` (100%) |
| RUNNABLE | 7.2% | `<empty stack>` (98.8%) |

**Interpretation:** The parser spends **92.8% of wall-clock time blocked or waiting** due
to the `supervisedWithLog` wrapper. The Ox `supervised` scope:
- Creates a virtual thread via `Continuation.run` (visible at 9% of WAITING)
- Parks the main thread via `CompletableFuture.get()` -> `Unsafe.park`
- The actual parse work runs on the virtual thread but is invisible to the stack profiler
  because virtual thread stacks are captured separately

The RUNNABLE 7.2% with empty stacks is the actual parse loop running on the virtual thread,
but JMH's `-prof stack` cannot sample virtual thread frames (it samples platform threads).

## Mapping to Research Optimization Targets

| Target | Research Prediction | Profiling Confirmation | Priority |
|--------|-------------------|----------------------|----------|
| **1. Remove `supervisedWithLog`** | HIGH impact, LOW risk | **CONFIRMED - CRITICAL.** 92.8% of time in Unsafe.park/Continuation overhead. Removing this is the single highest-impact change. | P0 |
| **2. Replace `lexems :+ Lexeme.EOF`** | MEDIUM impact, LOW risk | **Indeterminate.** GC profiler shows massive allocation but cannot distinguish EOF append from loop allocation. Worth addressing. | P1 |
| **3. Reduce `stack.take(k).map(_.node).reverse`** | MEDIUM impact, MEDIUM risk | **CONFIRMED.** iterative_json (more reductions) allocates 3.6x more than recursive_json (fewer reductions). This pattern is the dominant allocation source per reduction. | P1 |
| **4. Mutable ArrayDeque stack** | LOW-MEDIUM impact, MEDIUM risk | **Supported.** The 17-63 MB/op allocation is consistent with per-push tuple+cons allocation on every shift/reduction. | P2 |
| **5. Pre-computed Terminal lookup** | LOW impact, LOW risk | **Not visible.** Terminal allocation is likely a small fraction of total. JIT may optimize it. | P3 |
| **6. Array2D ParseTable** | LOW impact, HIGH risk | **Not warranted.** HashMap lookup is not visible in top stack frames. The `supervisedWithLog` overhead dwarfs any HashMap cost. | Skip |

## Recommendations for Plan 02

### Must Do (P0)
1. **Remove `supervisedWithLog` from `unsafeParse`** - This is non-negotiable. The parser
   spends 92.8% of its time in virtual thread infrastructure overhead. Expected speedup: 5-10x.
   The `Log` dependency in `ParseTable.apply` can be satisfied by removing `(using Log)` from
   the runtime path and using plain string interpolation in the error case.

### Should Do (P1)
2. **Replace `lexems :+ Lexeme.EOF` with Array conversion** - Convert input list to Array
   with EOF sentinel once at start. Eliminates O(n) append and enables O(1) indexed access.
3. **Optimize `stack.take(k).map(_.node).reverse`** - Build children array directly from
   stack in a single traversal. The 3.6x allocation difference between iterative and recursive
   JSON confirms this is the primary per-reduction allocation source.

### Consider (P2)
4. **Mutable stack** - Replace immutable `List[(Int, Node)]` with mutable structure. This
   reduces per-push allocation but requires converting `@tailrec` to `while` loop.

### Skip
5. Terminal pre-computation - Marginal benefit, JIT likely handles it
6. Array2D ParseTable - High risk, low payoff after supervisedWithLog removal

## Raw Data Files

- `gc_profile.txt` - GC profiler output for recursive_json
- `gc_iterative_json.txt` - GC profiler output for iterative_json
- `stack_profile.txt` - Stack profiler output for recursive_json
- `stack_iterative_json.txt` - Stack profiler output for iterative_json
