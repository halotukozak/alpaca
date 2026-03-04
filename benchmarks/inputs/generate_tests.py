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


# Token names for big-grammar: 2-char codes ka-kz (26) plus la-ld (4) = 30 tokens.
# These match the Alpaca/Fastparse BigGrammar implementations which use short codes
# to avoid prefix shadowing in the regex lexer (tok1 would shadow tok10).
TOKENS = [
    "ka", "kb", "kc", "kd", "ke", "kf", "kg", "kh", "ki", "kj",
    "kk", "kl", "km", "kn", "ko", "kp", "kq", "kr", "ks", "kt",
    "ku", "kv", "kw", "kx", "ky", "kz", "la", "lb", "lc", "ld",
]

# Generates big-grammar input files containing whitespace-separated token
# keywords from {ka..kz, la..ld} plus integers, arranged in valid sequences
# that the big-grammar parser rules accept across all three libraries.
#
# The generated patterns cycle through:
#   - pair rules:    ka kb, kc kd, ..., lc ld
#   - triple rules:  ka kb kc, kd ke kf, ...
#   - quad rules:    ka kb kc kd, ke kf kg kh, ...
#   - quint rules:   ka kb kc kd ke, kf kg kh ki kj, ...
#   - numeric rules: 42, ka 99, kk 55, ku 77
#   - single tokens: ka, kf, kk, kp, ku
T = TOKENS  # short alias for readability
PATTERNS = [
    # Pair rules (15 patterns)
    f"{T[0]} {T[1]}", f"{T[2]} {T[3]}", f"{T[4]} {T[5]}", f"{T[6]} {T[7]}", f"{T[8]} {T[9]}",
    f"{T[10]} {T[11]}", f"{T[12]} {T[13]}", f"{T[14]} {T[15]}", f"{T[16]} {T[17]}", f"{T[18]} {T[19]}",
    f"{T[20]} {T[21]}", f"{T[22]} {T[23]}", f"{T[24]} {T[25]}", f"{T[26]} {T[27]}", f"{T[28]} {T[29]}",
    # Triple rules (10 patterns)
    f"{T[0]} {T[1]} {T[2]}", f"{T[3]} {T[4]} {T[5]}", f"{T[6]} {T[7]} {T[8]}",
    f"{T[9]} {T[10]} {T[11]}", f"{T[12]} {T[13]} {T[14]}", f"{T[15]} {T[16]} {T[17]}",
    f"{T[18]} {T[19]} {T[20]}", f"{T[21]} {T[22]} {T[23]}", f"{T[24]} {T[25]} {T[26]}",
    f"{T[27]} {T[28]} {T[29]}",
    # Quad rules (10 patterns)
    f"{T[0]} {T[1]} {T[2]} {T[3]}", f"{T[4]} {T[5]} {T[6]} {T[7]}",
    f"{T[8]} {T[9]} {T[10]} {T[11]}", f"{T[12]} {T[13]} {T[14]} {T[15]}",
    f"{T[16]} {T[17]} {T[18]} {T[19]}", f"{T[20]} {T[21]} {T[22]} {T[23]}",
    f"{T[24]} {T[25]} {T[26]} {T[27]}",
    f"{T[0]} {T[5]} {T[10]} {T[15]}", f"{T[1]} {T[6]} {T[11]} {T[16]}", f"{T[2]} {T[7]} {T[12]} {T[17]}",
    # Quint rules (10 patterns)
    f"{T[0]} {T[1]} {T[2]} {T[3]} {T[4]}", f"{T[5]} {T[6]} {T[7]} {T[8]} {T[9]}",
    f"{T[10]} {T[11]} {T[12]} {T[13]} {T[14]}", f"{T[15]} {T[16]} {T[17]} {T[18]} {T[19]}",
    f"{T[20]} {T[21]} {T[22]} {T[23]} {T[24]}", f"{T[25]} {T[26]} {T[27]} {T[28]} {T[29]}",
    f"{T[0]} {T[3]} {T[6]} {T[9]} {T[12]}", f"{T[1]} {T[4]} {T[7]} {T[10]} {T[13]}",
    f"{T[2]} {T[5]} {T[8]} {T[11]} {T[14]}", f"{T[15]} {T[18]} {T[21]} {T[24]} {T[27]}",
    # Numeric rules (5 patterns)
    "42", "99 77", f"{T[0]} 55", f"{T[10]} 88", f"{T[20]} 33",
    # Single-token rules (5 patterns)
    f"{T[0]}", f"{T[5]}", f"{T[10]}", f"{T[15]}", f"{T[20]}",
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
