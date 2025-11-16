from typing import Optional

import AST
import TypeSystem as TS
from AST import SymbolRef
from Result import Result, Warn, Success, Failure
from TypeSystem import VarArg, Type


def prepare(dict_: dict[str, Type]) -> dict[str, SymbolRef]:
    return {name: SymbolRef(name, None, type_) for name, type_ in dict_.items()}


unary_numerical_type = TS.Function(TS.Int(), TS.Int()) | TS.Function(TS.Float(), TS.Float())
unary_vector_type = TS.FunctionTypeFactory(
    args=TS.Vector(),
    result_hint=TS.Vector(),
    result_type_factory=lambda expr: Success(expr.type)
)
unary_matrix_type = TS.FunctionTypeFactory(
    args=TS.Matrix(),
    result_hint=TS.Matrix(),
    result_type_factory=lambda expr: Success(expr.type)
)

binary_numerical_type = TS.Function((TS.Int(), TS.Int()), TS.Int()) \
                        | TS.Function((TS.numerical(), TS.numerical()), TS.Float())

binary_numerical_condition_type = TS.Function((TS.numerical(), TS.numerical()), TS.Bool())


def binary_metrix_type_factory(first: AST.Expr[TS.Matrix], second: AST.Expr[TS.Matrix]) -> Result[TS.Type]:
    a, b = first.type, second.type
    assert isinstance(a, TS.Matrix) and isinstance(b, TS.Matrix)

    errors = []
    warns = []

    rows: Optional[int] = None
    cols: Optional[int] = None

    if a.rows is None or b.rows is None:
        warns.append("Matrix rows could not be inferred")
    elif a.rows != b.rows:
        errors.append(f"Matrix rows mismatch: {a.rows} != {b.rows}")
    else:
        rows = a.rows

    if a.cols is None or b.cols is None:
        warns.append("Matrix columns could not be inferred")
    elif a.cols != b.cols:
        errors.append(f"Matrix columns mismatch: {a.cols} != {b.cols}")
    else:
        cols = a.cols

    if errors:
        return Failure(TS.Matrix(rows, cols), "\n".join(errors))
    elif warns:
        return Warn(TS.Matrix(rows, cols), "\n".join(warns))
    else:
        return Success(TS.Matrix(a.rows, a.cols))


def binary_vector_type_factory(first: AST.Expr[TS.Vector], second: AST.Expr[TS.Vector]) -> Result[TS.Type]:
    a, b = first.type, second.type
    assert isinstance(a, TS.Vector) and isinstance(b, TS.Vector)
    if a.arity is None or b.arity is None:
        return Warn(TS.Matrix(), "Vector arity could not be inferred")
    elif a.arity != b.arity:
        return Failure(TS.Matrix(), f"Vector lengths mismatch: {a.arity} != {b.arity}")
    else:
        return Success(TS.Matrix(a.arity, a.arity))


binary_matrix_type = TS.FunctionTypeFactory(
    args=(TS.Matrix(), TS.Matrix()),
    result_hint=TS.Matrix(),
    result_type_factory=binary_metrix_type_factory
)

binary_vector_type = TS.FunctionTypeFactory(
    args=((TS.Vector()), (TS.Vector())),
    result_hint=(TS.Vector()),
    result_type_factory=binary_vector_type_factory
)

scalar_type = TS.FunctionTypeFactory(
    args=(TS.Matrix(), TS.numerical()),
    result_hint=TS.Matrix(),
    result_type_factory=lambda expr, args: Success(expr.type)
) | TS.FunctionTypeFactory(
    args=(TS.Vector(), TS.numerical()),
    result_hint=TS.Vector(),
    result_type_factory=lambda expr, args: Success(expr.type)
)


def matrix_create_function_type(size: AST.Expr[TS.Int]) -> Result[TS.Type]:
    match size:
        case AST.Literal(n):
            return Success(TS.Matrix(n, n))
        case _:
            return Warn(TS.Matrix(), "Matrix size could not be inferred")


matrix_type = TS.FunctionTypeFactory(
    args=TS.Int(),
    result_hint=TS.Matrix(),
    result_type_factory=matrix_create_function_type
)

unary = prepare({
    "UMINUS": unary_numerical_type | unary_vector_type | unary_matrix_type,
    "'": TS.FunctionTypeFactory(
        args=(TS.Matrix()),
        result_hint=(TS.Matrix()),
        result_type_factory=lambda expr: Success(TS.Matrix(expr.type.cols, expr.type.rows))
    ),
    "eye": matrix_type,
    "zeros": matrix_type,
    "ones": matrix_type,
})

binary = prepare({
    "+": binary_numerical_type,
    "-": binary_numerical_type,
    "*": binary_numerical_type | scalar_type | binary_matrix_type | TS.Function((TS.String(), TS.Int()), TS.String()),
    "/": binary_numerical_type | scalar_type,
    "==": binary_numerical_condition_type,
    "!=": binary_numerical_condition_type,
    "<=": binary_numerical_condition_type,
    ">=": binary_numerical_condition_type,
    ">": binary_numerical_condition_type,
    "<": binary_numerical_condition_type,
    ".+": binary_matrix_type | binary_vector_type,
    ".-": binary_matrix_type | binary_vector_type,
    ".*": binary_matrix_type | binary_vector_type,
    "./": binary_matrix_type | binary_vector_type,
})


def init_vector_factory(*args: AST.Expr[TS.Vector]) -> Result[TS.Type]:
    # assert all(isinstance(arg, TS.Vector) for arg in arg_types)
    arities = set(arg.type.arity for arg in args)  # type: ignore
    if len(arities) == 1:
        return Success(TS.Matrix(len(args), arities.pop()))
    else:
        if None not in arities:
            return Warn(TS.Matrix(len(args)), f"Vector arities {arities} are not the same")
        return Warn(TS.Matrix(), "Cannot infer matrix size")


var_args = prepare({
    "INIT": TS.FunctionTypeFactory(
        args=VarArg(TS.numerical()),
        result_hint=TS.Vector(),
        result_type_factory=lambda *args: Success(TS.Vector(len(args)))
    ) | TS.FunctionTypeFactory(
        args=VarArg(TS.Vector()),
        result_hint=TS.Matrix(),
        result_type_factory=init_vector_factory
    ),

    "PRINT": TS.Function(VarArg(TS.Any()), TS.unit()),
})

symbols: dict[str, SymbolRef] = {**unary, **binary, **var_args}


# todo: maybe split into two functions?
def get_symbol(name: str) -> SymbolRef:
    res = symbols[name]
    if isinstance(res, SymbolRef):
        return res.copy()
    else:
        return res
