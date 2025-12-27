package example
package runtime

import scala.math.Numeric.Implicits.infixNumericOps
import scala.math.Ordering.Implicits.infixOrderingOps

type DynamicFunction = PartialFunction[List[?], ?]

enum Function(val tpe: Type, val implementation: DynamicFunction):
  export implementation.apply
  // unary
  case UMINUS
    extends Function(
      unary_numerical_type | unary_vector_type | unary_matrix_type,
      {
        case List(a: Number) => -a
        case List(a: Vector) => -a
        case List(a: Matrix) => -a
      },
    )
  case TRANSPOSE
    extends Function(
      Type.OverloadedFunction(
        arg = Type.Matrix(),
        resultHint = Type.Matrix(),
        resultTypeFactory = expr =>
          val tpe = expr.tpe.asInstanceOf[Type.Matrix]
          Result.Success(Type.Matrix(tpe.cols, tpe.rows)),
      ),
      { case (m: Matrix) *: EmptyTuple =>
        val rows = m.toArray.length
        val cols = m.toArray.head.toArray.length
        Matrix.tabulate(cols, rows)((i, j) => m(j)(i))
      },
    )
  case zeros
    extends Function(
      Type.MatrixInit,
      { case List(n: Int) => Matrix(Array.fill(n)(Vector(Array.fill(n)(Number(0))))) },
    )
  case ones
    extends Function(
      Type.MatrixInit,
      { case List(n: Int) => Matrix(Array.fill(n)(Vector(Array.fill(n)(Number(1))))) },
    )
  case eye
    extends Function(
      matrix_type,
      { case (n: Int) *: EmptyTuple =>
      Type.MatrixInit,
      { case List(n: Int) =>
        Matrix.tabulate(n, n)((i, j) => if i == j then Number(1) else Number(0))
      },
    )

  // binary
  case + extends Function(Type.binary.Numerical, { case List(a: Number, b: Number) => a + b })
  case - extends Function(Type.binary.Numerical, { case List(a: Number, b: Number) => a - b })
  case *
    extends Function(
      Type.binary.Numerical | Type.Scalar | Type.binary.Matrix | Type.Function(Tuple(Type.String), Type.Int),
      {
        case List(a: Number, b: Number) => a * b
        case List(a: Matrix, b: Number) => a.toArray.map(_.toArray.map(_ * b))
        case List(a: Vector, b: Number) => a.toArray.map(_ * b)
      },
    )
  case / extends Function(Type.binary.Numerical | Type.Scalar, { case List(a: Number, b: Number) => a / b })
  case == extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a == b })
  case != extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a != b })
  case <= extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a <= b })
  case >= extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a >= b })
  case < extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a < b })
  case > extends Function(Type.binary.Conditional, { case List(a: Number, b: Number) => a > b })
  case DOTADD extends Function(Type.binary.Matrix | Type.binary.Vector, { case List(a: Matrix, b: Matrix) => a + b })
  case DOTSUB extends Function(Type.binary.Matrix | Type.binary.Vector, { case List(a: Matrix, b: Matrix) => a - b })
  case DOTMUL extends Function(Type.binary.Matrix | Type.binary.Vector, { case List(a: Matrix, b: Matrix) => a * b })
  case DOTDIV extends Function(Type.binary.Matrix | Type.binary.Vector, { case List(a: Matrix, b: Matrix) => a / b })

  // varargs
  case PRINT
    extends Function(
      Type.Function(Type.VarArg(Type.Any), Type.Unit),
      { args =>
        println(
          args
            .map:
              case m: Matrix => m.toArray.map(_.toArray.mkString("[", ", ", "]")).mkString("[", ", ", "]")
              case v: Vector => v.toArray.mkString("[", ", ", "]")
              case x => x
            .mkString(" "),
        )
      },
    )

  case INIT
    extends Function(
      Type.OverloadedFunction(
        args = Type.VarArg(Type.Numerical),
        resultHint = Type.Vector(),
        resultTypeFactory = args => Result.Success(Type.Vector(args.size)),
      ) | Type.OverloadedFunction(
        args = Type.VarArg(Type.Vector()),
        resultHint = Type.Matrix(),
        resultTypeFactory = args =>
          args.map(_.tpe.asInstanceOf[Type.Vector].arity).distinct match
            case Seq(arity) =>
              Result.Success(Type.Matrix(args.size, arity))
            case arities =>
              val e = args.head
              if !arities.contains(null) then
                Result.warn(e.line)(Type.Matrix(args.size), s"Vector arities $arities are not the same")
              else Result.warn(e.line)(Type.Matrix(), "Cannot infer matrix size"),
      ),
      {
        case args @ List((_: Number), _*) => Vector(args.map(_.asInstanceOf[Number]))
        case args @ List((_: Vector), _*) => Matrix(args.map(_.asInstanceOf[Vector]))
      },
    )

object Function:
  def unapply(func: Function): (String, func.implementation.type) = (func.productPrefix, func.implementation)
