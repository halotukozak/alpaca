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

    print(f"✓ Wygenerowano {output_file}: {num_lines} linii")

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

    print(f"✓ Wygenerowano {output_file}: głębokość {num_lines}")

# {
#   "items": [
#     {
#       "id": 0,
#       "name": "item_0",
#       "value": 0,
#       "active": true
#     },
#     {
#       "id": 1,
#       "name": "item_1",
#       "value": 10,
#       "active": false
#     },
#    ...
#   ]
# }
def generate_iterative_json(num_objects: int, output_file: str):
    data = {
        "items": [
            {
                "id": i,
                "name": f"item_{i}",
                "value": i * 10,
                "active": i % 2 == 0
            }
            for i in range(num_objects)
        ]
    }

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

    print(f"✓ Wygenerowano {output_file}: {num_objects} obiektów")

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

    print(f"✓ Wygenerowano {output_file}: {num_objects} obiektów")

if __name__ == "__main__":
    tests = [
        (generate_iterative_math, "iterative_math"),
        (generate_recursive_math, "recursive_math"),
        (generate_iterative_json, "iterative_json"),
        (generate_recursive_json, "recursive_json"),
    ]

    for func, description in tests:
        print(f"\nGenerowanie testów: {description}\n")
        for size in (3, 100, 500, 1_000, 2_000, 5_000, 10_000):
            output_file = f"{description}_{size}.txt"
            func(size, output_file)

    print("\nGotowe!")
