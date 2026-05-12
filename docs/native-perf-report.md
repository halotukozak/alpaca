# Native Milestone — Performance Report

End-to-end performance comparison between the legacy lexer (using `dregex` for shadow
checks and `java.util.regex.Pattern` for tokenization) and the new cross-platform
implementation introduced in the `Native` milestone.

The runtime tokenizer was rewritten as an internal Brzozowski-derivative DFA driver
(`TokenMatcher`) so Alpaca can compile to Scala Native. The driver is then optimized
in a series of PRs.

## Setup

- Hardware: local dev machine (uncontrolled)
- JMH: 5 warmup + 5 measurement iterations, single fork, 2s per iter
- All numbers are `avgt` (average time per op)

## Micro-benchmark: `RegexMatcherBenchmark`

Hot tokenization loop on a CalcLexer-style pattern set (14 alternation branches:
identifiers, numbers, operators, whitespace, comments). Strategies compared on the
same generated input:

- `javaRegex` — `Pattern.matcher.lookingAt()` against the OR-joined named-group regex
  (the pre-milestone strategy)
- `tokenMatcher` — `TokenMatcher.matchAt` (the new internal driver)

### Final results

| input size | `javaRegex` | `tokenMatcher` | ratio |
|---|---|---|---|
|   100 chars |  2.17 µs/op |  **1.72 µs/op** | **0.79×** (faster) |
|  1000 chars |  19.7 µs/op | **13.50 µs/op** | **0.68×** (faster) |
| 10000 chars |   199 µs/op | **118.39 µs/op** | **0.60×** (faster) |

### Optimization progression (`tokenMatcher` only, input size = 10000)

| PR | optimization | µs/op | ratio vs `javaRegex` |
|---|---|---|---|
| #393 | per-pair derivative `HashMap` cache | 9322 | ~50× slower |
| #394 | ASCII fast-path `Array` cache | 7366 | ~40× slower |
| #395 | in-place `Array[Subset]` state | 5974 | ~35× slower |
| #396 | lazy DFA with interned state IDs | **118** | **0.60× (faster)** |

The decisive jump comes from #396: each unique `Vector[Subset]` state is interned to
an integer ID the first time it is reached, and transitions are stored in flat arrays
indexed by state ID. After warm-up the hot loop is pure integer array indexing — no
`Subset` equality, no `HashMap` probes, no per-character allocation.

## End-to-end benchmark: `AlpacaBenchmark.lex`

Full `tokenize()` pipeline on real generated input files. Compares the `master`
branch (legacy implementation) against the head of the milestone stack (`#397`).

| scenario | size | master (java.regex) | new (Opt3 + no-alloc) | delta |
|---|---|---|---|---|
| iterative_math |  1000 | 0.80 ms | 0.62 ms | **-22%** (faster) |
| iterative_math | 10000 | 12.8 ms | 8.50 ms | **-34%** (faster) |
| recursive_math |  1000 | 1.75 ms | 0.90 ms | **-49%** (faster) |
| recursive_math | 10000 | 14.9 ms | 10.0 ms | **-33%** (faster) |
| iterative_json |  1000 | 6.96 ms | 1.80 ms | **-74%** (faster) |
| iterative_json | 10000 | 67.2 ms | 59.7 ms | -11% (high variance) |
| **recursive_json** |  1000 | 9.93 ms | 21.2 ms | **+114%** (slower) |
| **recursive_json** | 10000 |  380 ms | 2062 ms | **+443%** (slower) |
| big_grammar |  1000 | 0.30 ms | 0.11 ms | **-63%** (faster) |
| big_grammar | 10000 | 3.02 ms | 1.11 ms | **-63%** (faster) |

### Interpretation

- The new matcher wins on 8/10 cases, often by a wide margin (33–74% faster)
- `recursive_json` is the pathological case: `recursive_json_10000.txt` is **600 MB**
  of deeply nested JSON. Master clocks **1.6 GB/s** there (JIT-optimized
  `java.util.regex.Pattern` plus internal regex-engine fast paths). The new matcher
  runs at **136 MB/s** on the same input
- The `Some` / named-tuple / `Integer.valueOf` allocations on every accepting state
  were a major source of overhead on JSON-shaped inputs (every digit of every number
  token allocates). PR #397 tracks the running longest match as primitive `Int`s
  and reduced this category by ~45% on `recursive_json`
- The crossover point for ASCII workloads is roughly 2–3k tokens. Below that the
  lazy DFA build cost is not fully amortized

## Native milestone goal

Achieved. After the milestone:

- `grep -rn 'java\.util\.regex' src/ test/` returns nothing
- `dregex` / `jparsec` / `slf4j-*` dropped from `build.mill`
- Build now provides both `jvm` and `native` modules; the full test suite passes on
  both (`./mill jvm.test` and `./mill native.test`)

## Future work

The remaining regression on `recursive_json` and the small-input crossover can be
removed by precomputing the DFA at compile time and lifting the transition tables
into macro-generated code (no runtime warm-up). The first attempt (branch
`perf/regex-matcher-compile-time-dfa`, not shipped) hit two issues that need
addressing before it lands:

1. **`ToExpr` lift overflows on complex lexers.** Lifting `Vector[Subset]` for each
   state expands recursively through the `Regex` AST. For lexers with ~30 patterns
   the cumulative quoted AST exhausts the macro typer stack even with `-Xss64m`.
2. **State-space blow-up.** Some patterns (notably `.`-heavy ones) produce hundreds
   of reachable ASCII states. Eager enumeration plus per-state lifting compounds the
   `ToExpr` issue.

Possible mitigations:

- Deduplicate `Subset` values before lifting and reference them by index
- Serialize the DFA to `Array[Byte]` and lift the bytes (a single `ToExpr[Array[Byte]]`
  call) — deserialize at construction time
- Cap state enumeration and fall back to lazy build above the cap

Another open question is the per-character throughput gap on huge inputs (`master`
gets 5–10× faster per character than the new matcher once the JIT fully warms on
megabytes of regex-heavy text). Closing that requires either profile-guided
specialization or replacing the interpreted state machine with a generated one.

## PR stack

Merge order (`master` ← bottom):

1. **#388** `feat(lexer): add cross-platform regex algebra`
2. **#389** `refactor(RegexChecker): drop dregex, use cross-platform Subset`
3. **#390** `refactor(lexer): replace java.util.regex with internal DFA driver`
4. **#391** `build(mill): split into jvm and native modules`
5. **#392** `perf(bench): compare java.util.regex vs internal TokenMatcher`
6. **#393** `perf(TokenMatcher): cache Brzozowski derivatives`
7. **#394** `perf(TokenMatcher): ASCII fast-path in derivative cache`
8. **#395** `perf(TokenMatcher): in-place Array[Subset] state`
9. **#396** `perf(TokenMatcher): lazy DFA with interned state IDs`
10. **#397** `perf(TokenMatcher): track last accept as primitive ints`

All on the [`Native` milestone](https://github.com/halotukozak/alpaca/milestone/8).
