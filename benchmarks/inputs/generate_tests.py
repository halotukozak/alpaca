from pathlib import Path

def indent(text: str, level: int) -> str:
    return '  ' * level + text

# 1 + 1 - 1 +
# 1 + 1 - 1 +
# ...
# 1 + 1 - 1
def generate_iterative_math(num_lines: int, output_file: str):
    with open(output_file, 'w') as f:
        for _ in range(num_lines-1):
            f.write("1 + 1 - 1 +\n")
        f.write("1 + 1 - 1\n")

    print(f"  Generated {output_file}: {num_lines} lines")

# 1 + 1 + (
# 1 + 1 + (
# ...
# 0
# ) - 1
# ) - 1
# ...
def generate_recursive_math(num_lines: int, output_file: str):
    with open(output_file, 'w') as f:
        for _ in range(num_lines):
            f.write("1 + 1 + (\n")
        f.write("0\n")
        for _ in range(num_lines):
            f.write(") - 1\n")

    print(f"  Generated {output_file}: depth {num_lines}")

# {
#   "items": [
#     {
#       "id": 0,
#       "name": "item_0",
#       "value": 0,
#       "active": true
#     },
#     ...
#   ]
# }
def generate_iterative_json(num_objects: int, output_file: str):
    with open(output_file, 'w') as f:
        f.write("{\n")
        f.write(indent("\"items\": [\n", 1))
        for i in range(num_objects):
            f.write(indent("{\n", 2))
            f.write(indent(f"\"id\": {i},\n", 3))
            f.write(indent(f"\"name\": \"item_{i}\",\n", 3))
            f.write(indent(f"\"value\": {i * 10},\n", 3))
            f.write(indent(f"\"active\": {'true' if i % 2 == 0 else 'false'}\n", 3))
            if i < num_objects - 1:
                f.write(indent("},\n", 2))
            else:
                f.write(indent("}\n", 2))
        f.write(indent("]\n", 1))
        f.write("}")

    print(f"  Generated {output_file}: {num_objects} objects")

def generate_recursive_json(num_objects: int, output_file: str):
    with open(output_file, 'w') as f:
        f.write("{\n")
        for i in range(num_objects):
            f.write(indent(f"\"id\": {i},\n", i + 1))
            f.write(indent(f"\"name\": \"obj{i}\",\n", i + 1))
            f.write(indent(f"\"value\": {i * 10},\n", i + 1))
            f.write(indent(f"\"active\": {'true' if i % 2 == 0 else 'false'},\n", i + 1))
            f.write(indent("\"child\": {\n", i + 1))

        for i in reversed(range(num_objects + 1)):
            f.write(indent("}\n", i + 1))

    print(f"  Generated {output_file}: {num_objects} objects")


# Generates big-grammar input files containing whitespace-separated token
# keywords from {tok0..tok29} plus integers, arranged in valid sequences
# that the big-grammar parser rules accept across all three libraries.
#
# The generated patterns cycle through:
#   - pair rules:    tok0 tok1, tok2 tok3, ..., tok28 tok29
#   - triple rules:  tok0 tok1 tok2, tok3 tok4 tok5, ...
#   - quad rules:    tok0 tok1 tok2 tok3, tok4 tok5 tok6 tok7, ...
#   - quint rules:   tok0 tok1 tok2 tok3 tok4, tok5 tok6 tok7 tok8 tok9, ...
#   - numeric rules: 42, tok0 99, tok10 55, tok20 77
#   - single tokens: tok0, tok5, tok10, tok15, tok20
PATTERNS = [
    # Pair rules (15 patterns)
    "tok0 tok1", "tok2 tok3", "tok4 tok5", "tok6 tok7", "tok8 tok9",
    "tok10 tok11", "tok12 tok13", "tok14 tok15", "tok16 tok17", "tok18 tok19",
    "tok20 tok21", "tok22 tok23", "tok24 tok25", "tok26 tok27", "tok28 tok29",
    # Triple rules (10 patterns)
    "tok0 tok1 tok2", "tok3 tok4 tok5", "tok6 tok7 tok8",
    "tok9 tok10 tok11", "tok12 tok13 tok14", "tok15 tok16 tok17",
    "tok18 tok19 tok20", "tok21 tok22 tok23", "tok24 tok25 tok26",
    "tok27 tok28 tok29",
    # Quad rules (10 patterns)
    "tok0 tok1 tok2 tok3", "tok4 tok5 tok6 tok7",
    "tok8 tok9 tok10 tok11", "tok12 tok13 tok14 tok15",
    "tok16 tok17 tok18 tok19", "tok20 tok21 tok22 tok23",
    "tok24 tok25 tok26 tok27",
    "tok0 tok5 tok10 tok15", "tok1 tok6 tok11 tok16", "tok2 tok7 tok12 tok17",
    # Quint rules (10 patterns)
    "tok0 tok1 tok2 tok3 tok4", "tok5 tok6 tok7 tok8 tok9",
    "tok10 tok11 tok12 tok13 tok14", "tok15 tok16 tok17 tok18 tok19",
    "tok20 tok21 tok22 tok23 tok24", "tok25 tok26 tok27 tok28 tok29",
    "tok0 tok3 tok6 tok9 tok12", "tok1 tok4 tok7 tok10 tok13",
    "tok2 tok5 tok8 tok11 tok14", "tok15 tok18 tok21 tok24 tok27",
    # Numeric rules (5 patterns)
    "42", "99 77", "tok0 55", "tok10 88", "tok20 33",
    # Single-token rules (5 patterns)
    "tok0", "tok5", "tok10", "tok15", "tok20",
]


def generate_big_grammar(num_tokens: int, output_file: str):
    """Generate big-grammar input with approximately num_tokens token instances.

    Cycles through all valid patterns to exercise many parser rules.
    """
    with open(output_file, 'w') as f:
        tokens_written = 0
        pattern_idx = 0
        first = True
        while tokens_written < num_tokens:
            pattern = PATTERNS[pattern_idx % len(PATTERNS)]
            pattern_tokens = len(pattern.split())
            if not first:
                f.write("\n")
            f.write(pattern)
            tokens_written += pattern_tokens
            pattern_idx += 1
            first = False
        f.write("\n")

    print(f"  Generated {output_file}: ~{num_tokens} tokens")


if __name__ == "__main__":
    inputs_dir = Path(__file__).parent

    tests = [
        (generate_iterative_math, "iterative_math"),
        (generate_recursive_math, "recursive_math"),
        (generate_iterative_json, "iterative_json"),
        (generate_recursive_json, "recursive_json"),
        (generate_big_grammar, "big_grammar"),
    ]

    for func, description in tests:
        print(f"\nGenerating tests: {description}\n")
        for size in (3, 100, 500, 1_000, 2_000, 5_000, 10_000):
            output_file = str(inputs_dir / f"{description}_{size}.txt")
            func(size, output_file)

    print("\nDone!")
