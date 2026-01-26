import time
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent))

from math_parser import MathLexer, MathParser
from json_parser import JsonLexer, JsonParser

def benchmark_lexer(lexer_class, content, iterations=10):
    # Warmup
    for _ in range(3):
        list(lexer_class().tokenize(content))

    # Benchmark
    start = time.perf_counter()
    for _ in range(iterations):
        list(lexer_class().tokenize(content))
    end = time.perf_counter()

    return end - start

def benchmark_parser(lexer_class, parser_class, content, iterations=10):
    tokens = list(lexer_class().tokenize(content))

    # Warmup
    for _ in range(3):
        parser_class().parse(iter(tokens))
    # Benchmark
    start = time.perf_counter()
    for _ in range(iterations):
        parser_class().parse(iter(tokens))
    end = time.perf_counter()

    return end - start

def benchmark_full_parse(lexer_class, parser_class, content, iterations=10):
    # Warmup
    for _ in range(3):
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
    
    inputs_dir = Path(__file__).parent.parent / "inputs"
    outputs_dir = Path(__file__).parent.parent / "outputs"

    for group_name, lexer_class, parser_class in test_groups:
        with (outputs_dir / f"sly_{group_name}.csv").open('w') as result_file:
            result_file.write("Size,Lex Time,Lex Iterations,Parse Time,Parse Iterations,Full Parse Time,Full Parse Iterations\n")

            for size in [100, 500, 1_000, 2_000]:
                with (inputs_dir / f"{group_name}_{size}.txt").open('r') as f:
                    content = f.read()
                
                result_file.write(f"{size}")
                t = benchmark_lexer(lexer_class, content)
                result_file.write(f",{format_time(t)},{10}")
                t = benchmark_parser(lexer_class, parser_class, content)
                result_file.write(f",{format_time(t)},{10}")
                t = benchmark_full_parse(lexer_class, parser_class, content)
                result_file.write(f",{format_time(t)},{10}")
                result_file.write("\n")

                print(f"Completed {group_name} size {size}")
