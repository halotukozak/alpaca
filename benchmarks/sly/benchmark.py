import time
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent))

from math_parser import MathLexer, MathParser
from json_parser import JsonLexer, JsonParser

ITERATIONS = 10
WARMUP_ITERATIONS = 3
TEST_SIZES = [100, 500, 1_000, 2_000, 5_000, 10_000]

def benchmark_lexer(lexer_class, content, iterations=ITERATIONS):
    # Warmup
    for _ in range(WARMUP_ITERATIONS):
        list(lexer_class().tokenize(content))

    # Benchmark
    start = time.perf_counter()
    for _ in range(iterations):
        list(lexer_class().tokenize(content))
    end = time.perf_counter()

    return end - start

def benchmark_parser(lexer_class, parser_class, content, iterations=ITERATIONS):
    tokens = list(lexer_class().tokenize(content))

    # Warmup
    for _ in range(WARMUP_ITERATIONS):
        parser_class().parse(iter(tokens))
    # Benchmark
    start = time.perf_counter()
    for _ in range(iterations):
        parser_class().parse(iter(tokens))
    end = time.perf_counter()

    return end - start

def benchmark_full_parse(lexer_class, parser_class, content, iterations=ITERATIONS):
    # Warmup
    for _ in range(WARMUP_ITERATIONS):
        tokens = lexer_class().tokenize(content)
        parser_class().parse(tokens)

    # Benchmark
    start = time.perf_counter()
    for _ in range(iterations):
        tokens = lexer_class().tokenize(content)
        parser_class().parse(tokens)
    end = time.perf_counter()

    return end - start

def format_time(seconds):
    if seconds < 0.001:
        return f"{seconds * 1_000_000:.2f} µs"
    elif seconds < 1:
        return f"{seconds * 1_000:.2f} ms"
    else:
        return f"{seconds:.2f} s"

if __name__ == "__main__":
    test_groups = [
        ("iterative_math", MathLexer, MathParser),
        ("recursive_math", MathLexer, MathParser),
        ("iterative_json", JsonLexer, JsonParser),
        ("recursive_json", JsonLexer, JsonParser),
    ]

    benchmarks_dir = Path(__file__).parent.parent
    inputs_dir = benchmarks_dir / "inputs"
    outputs_dir = benchmarks_dir / "outputs"

    for group_name, lexer_class, parser_class in test_groups:
        with (outputs_dir / f"sly_{group_name}.csv").open('w') as result_file:
            result_file.write("Size,Lex Time,Lex Iterations,Parse Time,Parse Iterations,Full Parse Time,Full Parse Iterations\n")

            for size in TEST_SIZES:
                with (inputs_dir / f"{group_name}_{size}.txt").open('r') as f:
                    content = f.read()

                result_file.write(f"{size}")
                t = benchmark_lexer(lexer_class, content)
                result_file.write(f",{format_time(t)},{ITERATIONS}")
                t = benchmark_parser(lexer_class, parser_class, content)
                result_file.write(f",{format_time(t)},{ITERATIONS}")
                t = benchmark_full_parse(lexer_class, parser_class, content)
                result_file.write(f",{format_time(t)},{ITERATIONS}")
                result_file.write("\n")

                print(f"Completed {group_name} size {size}")
