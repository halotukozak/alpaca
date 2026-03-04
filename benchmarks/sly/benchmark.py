"""
SLY benchmark harness producing JMH-compatible JSON output.

Runs 5 warmup + 10 measurement iterations per scenario/size combination,
computing per-iteration statistics (mean, stdev, confidence intervals,
percentiles) and outputting a JSON array matching JMH's result format.

Output: benchmarks/outputs/sly_results.json
"""

import json
import math
import statistics
import sys
import time
from pathlib import Path

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent))

from math_parser import MathLexer, MathParser
from json_parser import JsonLexer, JsonParser
from big_grammar import BigGrammarLexer, BigGrammarParser

WARMUP = 5
MEASUREMENT = 10
TEST_SIZES = [100, 500, 1_000, 2_000, 5_000, 10_000]

# t-distribution critical value: t(9, 0.9995) for 99.9% CI with 9 degrees of freedom
T_VALUE_9DF = 4.587

SCENARIOS = [
    ("iterative_math", MathLexer, MathParser),
    ("recursive_math", MathLexer, MathParser),
    ("iterative_json", JsonLexer, JsonParser),
    ("recursive_json", JsonLexer, JsonParser),
    ("big_grammar", BigGrammarLexer, BigGrammarParser),
]

PARSER_CLASS_NAMES = {
    "iterative_math": "MathParser",
    "recursive_math": "MathParser",
    "iterative_json": "JsonParser",
    "recursive_json": "JsonParser",
    "big_grammar": "BigGrammarParser",
}


def benchmark_with_stats(func, *args):
    """Run warmup + measurement iterations, return JMH-compatible primaryMetric.

    Times each measurement iteration individually with time.perf_counter().
    All times are converted to milliseconds to match JMH @OutputTimeUnit(MILLISECONDS).
    """
    # Warmup (discard results)
    for _ in range(WARMUP):
        func(*args)

    # Measurement -- individual iteration times in milliseconds
    raw_times = []
    for _ in range(MEASUREMENT):
        start = time.perf_counter()
        func(*args)
        elapsed_ms = (time.perf_counter() - start) * 1000
        raw_times.append(elapsed_ms)

    mean = statistics.mean(raw_times)
    stdev = statistics.stdev(raw_times) if len(raw_times) > 1 else 0.0
    sorted_times = sorted(raw_times)
    n = len(raw_times)

    # 99.9% confidence interval using t-distribution
    stderr = stdev / math.sqrt(n)
    margin = T_VALUE_9DF * stderr

    percentiles = {
        "0.0": sorted_times[0],
        "50.0": statistics.median(raw_times),
        "90.0": sorted_times[int(0.9 * n) - 1] if n >= 10 else sorted_times[-1],
        "95.0": sorted_times[int(0.95 * n) - 1] if n >= 20 else sorted_times[-1],
        "99.0": sorted_times[-1],
        "99.9": sorted_times[-1],
        "100.0": sorted_times[-1],
    }

    return {
        "score": mean,
        "scoreError": margin,
        "scoreConfidence": [mean - margin, mean + margin],
        "scorePercentiles": percentiles,
        "scoreUnit": "ms/op",
        "rawData": [raw_times],
    }


def _make_jmh_entry(parser_class_name, method, scenario, size, primary_metric):
    """Build a JMH-format JSON entry for one benchmark combination."""
    return {
        "jmhVersion": "SLY-compat",
        "benchmark": f"sly.{parser_class_name}.{method}",
        "mode": "avgt",
        "threads": 1,
        "forks": 1,
        "warmupIterations": WARMUP,
        "measurementIterations": MEASUREMENT,
        "params": {
            "scenario": scenario,
            "size": str(size),
        },
        "primaryMetric": primary_metric,
        "secondaryMetrics": {},
    }


def _null_metric():
    """Return a null primaryMetric for failed benchmarks (RecursionError, etc.)."""
    return {
        "score": float('nan'),
        "scoreError": float('nan'),
        "scoreConfidence": [float('nan'), float('nan')],
        "scorePercentiles": {
            "0.0": float('nan'), "50.0": float('nan'), "90.0": float('nan'),
            "95.0": float('nan'), "99.0": float('nan'), "99.9": float('nan'),
            "100.0": float('nan'),
        },
        "scoreUnit": "ms/op",
        "rawData": [[]],
    }


def do_lex(lexer_class, content):
    """Lex the content and consume all tokens."""
    list(lexer_class().tokenize(content))


def do_parse(lexer_class, parser_class, content):
    """Pre-tokenize and then parse. Measures parse time only."""
    tokens = list(lexer_class().tokenize(content))
    parser_class().parse(iter(tokens))


def do_full_parse(lexer_class, parser_class, content):
    """Lex and parse in one pass (measures combined throughput)."""
    tokens = lexer_class().tokenize(content)
    parser_class().parse(tokens)


if __name__ == "__main__":
    benchmarks_dir = Path(__file__).parent.parent
    inputs_dir = benchmarks_dir / "inputs"
    outputs_dir = benchmarks_dir / "outputs"
    outputs_dir.mkdir(parents=True, exist_ok=True)

    results = []

    for scenario_name, lexer_class, parser_class in SCENARIOS:
        parser_class_name = PARSER_CLASS_NAMES[scenario_name]

        for size in TEST_SIZES:
            input_file = inputs_dir / f"{scenario_name}_{size}.txt"
            if not input_file.exists():
                print(f"SKIP {scenario_name} size {size}: input file not found")
                continue

            content = input_file.read_text()

            # Benchmark: lex
            try:
                metric = benchmark_with_stats(do_lex, lexer_class, content)
            except (RecursionError, Exception) as e:
                print(f"  FAIL lex {scenario_name} size {size}: {e}")
                metric = _null_metric()
            results.append(_make_jmh_entry(parser_class_name, "lex", scenario_name, size, metric))

            # Benchmark: parse (tokenize first, then parse only)
            try:
                metric = benchmark_with_stats(do_parse, lexer_class, parser_class, content)
            except (RecursionError, Exception) as e:
                print(f"  FAIL parse {scenario_name} size {size}: {e}")
                metric = _null_metric()
            results.append(_make_jmh_entry(parser_class_name, "parse", scenario_name, size, metric))

            # Benchmark: fullParse (lex + parse combined)
            try:
                metric = benchmark_with_stats(do_full_parse, lexer_class, parser_class, content)
            except (RecursionError, Exception) as e:
                print(f"  FAIL fullParse {scenario_name} size {size}: {e}")
                metric = _null_metric()
            results.append(_make_jmh_entry(parser_class_name, "fullParse", scenario_name, size, metric))

            print(f"Completed {scenario_name} size {size}")

    # Write all results as a single JSON array
    output_file = outputs_dir / "sly_results.json"
    with output_file.open('w') as f:
        json.dump(results, f, indent=2, allow_nan=True)

    print(f"\nDone! {len(results)} benchmark entries written to {output_file}")
