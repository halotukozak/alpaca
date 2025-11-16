from typing import Optional, Callable, Any, Iterator


class Vector:
    def __init__(self, source: list[int | float]):
        self.source = source

    def __setitem__(self, index: int, value: int | float) -> None:
        self.source[index] = value

    def __getitem__(self, index: int) -> int | float:
        return self.source[index]

    def __iter__(self) -> Iterator[int | float]:
        return iter(self.source)

    def __add__(self, other: 'Vector') -> 'Vector':
        return Vector([v + w for v, w in zip(self, other)])

    def __sub__(self, other: 'Vector') -> 'Vector':
        return Vector([v - w for v, w in zip(self, other)])

    def __mul__(self, other: 'Vector') -> 'Vector':
        return Vector([v * w for v, w in zip(self, other)])

    def __truediv__(self, other: 'Vector') -> 'Vector':
        return Vector([v / w for v, w in zip(self, other)])

    def __str__(self) -> str:
        return str(self.source)

    def __repr__(self) -> str:
        return str(self.source)


class Matrix:
    def __init__(self, source: list[Vector]):
        self.source = source

    def __setitem__(self, row: int, value: Vector) -> None:
        self.source[row] = value

    def __getitem__(self, row: int) -> Vector:
        return self.source[row]

    def __add__(self, other: 'Matrix') -> 'Matrix':
        return Matrix([v + w for v, w in zip(self.source, other.source)])

    def __sub__(self, other: 'Matrix') -> 'Matrix':
        return Matrix([v - w for v, w in zip(self.source, other.source)])

    def __mul__(self, other: 'Matrix') -> 'Matrix':
        return Matrix([v * w for v, w in zip(self.source, other.source)])

    def __truediv__(self, other: 'Matrix') -> 'Matrix':
        return Matrix([v / w for v, w in zip(self.source, other.source)])

    def __str__(self) -> str:
        return str(self.source)

    def __repr__(self) -> str:
        return repr(self.source)


class Env(object):
    memory: dict[str, Any] = {}
    functions: dict[str, Callable] = {}

    def __init__(self, parent: Optional['Env']):
        self.parent = parent

    def create[T](self, name: str, value: Optional[T] = None) -> None:
        self.memory[name] = value

    def update(self, name: str, value: Any) -> bool:
        if name in self.memory.keys():
            self.memory[name] = value
            return True
        return self.parent is not None and self.parent.update(name, value)

    def get_value(self, name: str) -> Any:
        if name in self.memory.keys():
            return self.memory[name]
        if self.parent:
            return self.parent.get_value(name)

    def get_function(self, name: str) -> Optional[Callable]:
        if name in self.functions.keys():
            return self.functions[name]
        if self.parent:
            return self.parent.get_function(name)
        return None


def init_lambda(*args: int | float | Vector) -> Vector | Matrix:
    if isinstance(args[0], int):
        return Vector(list(args))  # type: ignore
    else:
        return Matrix(list(args))  # type: ignore


class EnvTable(object):
    global_env = Env(None)
    global_env.functions = {
        '+': lambda a, b: a + b,
        '-': lambda a, b: a - b,
        '*': lambda a, b: a * b,
        '/': lambda a, b: a / b,
        'PRINT': lambda *args: print(*args),
        'zeros': lambda n: Matrix([Vector([0] * n) for _ in range(n)]),
        'ones': lambda n: Matrix([Vector([1] * n) for _ in range(n)]),
        'eye': lambda n: Matrix([Vector([1 if i == j else 0 for j in range(n)]) for i in range(n)]),
        'INIT': init_lambda,
        '==': lambda a, b: a == b,
        '!=': lambda a, b: a != b,
        '<=': lambda a, b: a <= b,
        '>=': lambda a, b: a >= b,
        '<': lambda a, b: a < b,
        '>': lambda a, b: a > b,
        '.+': lambda a, b: a + b,
        '.-': lambda a, b: a - b,
        '.*': lambda a, b: a * b,
        './': lambda a, b: a / b,
    }

    actual_env: Env = global_env

    def push_env(self, new_env: Env) -> None:
        self.actual_env = new_env

    def pop_env(self) -> None:
        assert self.actual_env.parent is not None
        self.actual_env = self.actual_env.parent
