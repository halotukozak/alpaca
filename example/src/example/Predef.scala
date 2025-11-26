package example

import java.security.DrbgParameters.Reseed
import scala.collection.mutable

val unary_numerical_type = Type.Function(Tuple(Type.Int), Type.Int) | Type.Function(Tuple(Type.Float), Type.Float)
val unary_vector_type = Type.FunctionTypeFactory(
  arg = Type.Vector(),
  resultHint = Type.Vector(),
  resultTypeFactory = expr => Result.Success(expr.tpe),
)
val unary_matrix_type = Type.FunctionTypeFactory(
  arg = Type.Matrix(),
  resultHint = Type.Matrix(),
  resultTypeFactory = expr => Result.Success(expr.tpe),
)

val binary_numerical_type = Type.Function((Type.Int, Type.Int), Type.Int) |
  Type.Function((Type.Numerical, Type.Numerical), Type.Float)

val binary_numerical_condition_type = Type.Function((Type.Numerical, Type.Numerical), Type.Bool)

val binary_matrix_type = Type.FunctionTypeFactory(
  args = (Type.Matrix(), Type.Matrix()),
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case (AST.Expr(a: Type.Matrix), AST.Expr(b: Type.Matrix)) =>
      val errors = mutable.Set.empty[String]
      val warns = mutable.Set.empty[String]

      var rows: Int | Null = null
      var cols: Int | Null = null

      if (a.rows == null) || (b.rows == null) then warns += "Matrix rows could not be inferred"
      else if a.rows != b.rows then errors += s"Matrix rows mismatch: ${a.rows} != ${b.rows}"
      else rows = a.rows

      if (a.cols == null) || (b.cols == null) then warns += "Matrix columns could not be inferred"
      else if a.cols != b.cols then errors += s"Matrix columns mismatch: ${a.cols} != ${b.cols}"
      else cols = a.cols

      if errors.nonEmpty then Result.error(Type.Matrix(rows, cols), errors.toSeq*)
      else if warns.nonEmpty then Result.warn(Type.Matrix(rows, cols), warns.toSeq*)
      else Result.Success(Type.Matrix(a.rows, a.cols))
    case _ => Result.error(Type.Matrix(), "Invalid arguments"),
)

val binary_vector_type = Type.FunctionTypeFactory(
  args = (Type.Vector(), Type.Vector()),
  resultHint = Type.Vector(),
  resultTypeFactory =
    case (AST.Expr(a: Type.Vector), AST.Expr(b: Type.Vector)) =>
      if (a.arity == null) || (b.arity == null) then Result.warn(Type.Matrix(), "Vector arity could not be inferred")
      else if a.arity != b.arity then Result.error(Type.Matrix(), s"Vector lengths mismatch: ${a.arity} != ${b.arity}")
      else Result.Success(Type.Matrix(a.arity, a.arity))
    case _ => Result.error(Type.Matrix(), "Invalid arguments"),
)

val scalar_type = Type.FunctionTypeFactory(
  args = (Type.Matrix(), Type.Numerical),
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case (expr, args) => Result.Success(expr.tpe),
) | Type.FunctionTypeFactory(
  args = (Type.Vector(), Type.Numerical),
  resultHint = Type.Vector(),
  resultTypeFactory =
    case (expr, args) => Result.Success(expr.tpe),
)

val matrix_type = Type.FunctionTypeFactory(
  arg = Type.Int,
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case AST.Literal(_, n: Int, _) => Result.Success(Type.Matrix(n, n))
    case _ => Result.warn(Type.Matrix(), "Matrix size could not be inferred"),
)

val symbols: Map[String, AST.SymbolRef] = Map(
  // unary
  "UMINUS" -> (unary_numerical_type | unary_vector_type | unary_matrix_type),
  "TRANSPOSE" -> Type.FunctionTypeFactory(
    arg = Type.Matrix(),
    resultHint = Type.Matrix(),
    resultTypeFactory = expr =>
      val tpe = expr.tpe.asInstanceOf[Type.Matrix]
      Result.Success(Type.Matrix(tpe.cols, tpe.rows)),
  ),
  "EYE" -> matrix_type,
  "ZEROS" -> matrix_type,
  "ONES" -> matrix_type,

  // binary
  "+" -> binary_numerical_type,
  "-" -> binary_numerical_type,
  "*" -> (binary_numerical_type | scalar_type | binary_matrix_type | Type.Function(Tuple(Type.String), Type.Int)),
  "/" -> (binary_numerical_type | scalar_type),
  "==" -> binary_numerical_condition_type,
  "!=" -> binary_numerical_condition_type,
  "<=" -> binary_numerical_condition_type,
  ">=" -> binary_numerical_condition_type,
  ">" -> binary_numerical_condition_type,
  "<" -> binary_numerical_condition_type,
  "DOTADD" -> (binary_matrix_type | binary_vector_type),
  "DOTSUB" -> (binary_matrix_type | binary_vector_type),
  "DOTMUL" -> (binary_matrix_type | binary_vector_type),
  "DOTDIV" -> (binary_matrix_type | binary_vector_type),

  // varargs
  "INIT" ->
    (Type.FunctionTypeFactory.varargs(
      arg = Type.VarArg(Type.Numerical),
      resultHint = Type.Vector(),
      resultTypeFactory = args => Result.Success(Type.Vector(args.size)),
    ) | Type.FunctionTypeFactory.varargs(
      arg = Type.VarArg(Type.Vector()),
      resultHint = Type.Matrix(),
      resultTypeFactory = args =>
        args.map(_.tpe.asInstanceOf[Type.Vector].arity).distinct match
          case Seq(arity) =>
            Result.Success(Type.Matrix(args.size, arity))
          case arities =>
            if !arities.contains(null) then
              Result.warn(Type.Matrix(args.size), f"Vector arities {arities} are not the same")
            else Result.warn(Type.Matrix(), "Cannot infer matrix size"),
    )),
  "PRINT" -> Type.Function(Tuple(Type.VarArg(Type.Any)), Type.Unit),
).map:
  case (name, tpe) => name -> AST.SymbolRef(tpe, name, -1)
