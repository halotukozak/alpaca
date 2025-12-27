package example

import scala.collection.mutable
import scala.compiletime.ops.boolean.*
import scala.compiletime.ops.int.*

sealed trait Type(val isFinal: Boolean = true):
  def |(other: Type): Type = Type.Or(this, other)

  def <=(other: Type): Boolean = other match
    case Type.Any => true
    case Type.AnyOf(types) => types.exists(this <= _)
    case _ => isSubtype(other)

  protected def isSubtype(other: Type): Boolean

  def >=(other: Type): Boolean = other <= this

object Type:
  type Undef = Undef.type
  type Unit = Unit.type
  type Any = Any.type
  type Int = Int.type
  type Float = Float.type
  type String = String.type
  type Bool = Bool.type
  type Range = Range.type

  type FromScala[T] = T match
    case scala.Int => Type.Int
    case scala.Double => Type.Float
    case scala.Unit => Type.Unit
    case scala.Boolean => Type.Bool
    case scala.Predef.String => Type.String

  type ToScala[T <: Type] = T match
    case Type.Int => scala.Int
    case Type.Float => scala.Double
    case Type.Unit => scala.Unit
    case Type.Bool => scala.Boolean
    case Type.String => scala.Predef.String

  val Numerical: Type = Int | Float
  type Numerical = Numerical.type

  def Or(left: Type, right: Type): Type = (left, right) match
    case (left: Any, _) => Any
    case (_, right: Any) => Any
    case (left: AnyOf, right: AnyOf) => AnyOf(left.all ++ right.all)
    case (left: AnyOf, right) => AnyOf(left.all + right)
    case (left, right: AnyOf) => AnyOf(right.all + left)
    case (left, right) if left == right => left
    case _ => AnyOf(Set(left, right))

  case class AnyOf(all: Set[Type]) extends Type(false):
    override def toString: Predef.String = all.mkString(" | ")

    override def <=(other: Type): Boolean = all.forall(_ <= other)

    override protected def isSubtype(other: Type): Boolean = false // Should not be called via <=

  type IsVarArg[X] <: Boolean = X match
    case Type.VarArg[?] => true
    case _ => false

  type IsValidTuple[X] <: Boolean = X match
    case EmptyTuple => true
    case Type *: t => IsValidTuple[t]
    case _ => false

  case class Function(
    args: Tuple | Type.VarArg[?],
    result: Type,
  )(using (IsVarArg[args.type] || IsValidTuple[args.type]) =:= true,
  ) extends Type:
    def takes(provided: List[Type]): Boolean = args match
      case _: EmptyTuple => provided.isEmpty
      case Type.VarArg(tpe) => provided.forall(_ <= tpe)
      case args: Tuple =>
        args.toList
          .zip(provided)
          .forall:
            case (a: Type, b: Type) => b <= a
            case _ => false

    override protected def isSubtype(other: Type): Boolean = (this, other) match
      case (Type.Function(args: Type.VarArg[?], result), Type.Function(args2: Type.VarArg[?], result2)) =>
        args <= args2 && result <= result2

      case (Type.Function(args: Tuple, result), Type.Function(args2: Tuple, result2)) =>
        args.zip(args2).toList.forall { case (a: Type, b: Type) => a <= b } && result <= result2

      case _ => false

  type Args[X <: Tuple | Type.VarArg[?]] = X match
    case Type.VarArg[tpe] => List[AST.Expr { type Tpe = tpe }]
    case h *: t => TupleFactory[Tuple.Size[h *: t], AST.Expr { type Tpe = h }]
    case EmptyTuple => Unit

  object OverloadedFunction:
    def apply[T <: Type](
      arg: T,
      resultHint: Type,
      resultTypeFactory: AST.Expr { type Tpe = T } => Result[Type],
    ): OverloadedFunction = new OverloadedFunction(
      Tuple(arg),
      resultHint,
      { case expr *: EmptyTuple => resultTypeFactory(expr) },
    )

  case class OverloadedFunction(
    args: Tuple | Type.VarArg[?],
    resultHint: Type,
    resultTypeFactory: Args[args.type] => Result[Type],
  )(using (IsVarArg[args.type] || IsValidTuple[args.type]) =:= true,
  ) extends Type(false):

    override def toString =
      val argsRepr = args match
        case EmptyTuple => "()"
        case Type.VarArg(tpe) => s"...$tpe"
        case t: Tuple => t.toList.mkString("(", ", ", ")")
      s"OverloadedFunction$argsRepr => $resultHint"

    private def unsafeResult(arguments: scala.Any) =
      resultTypeFactory(arguments.asInstanceOf[Args[args.type]]).map(Function(args, _))

    def apply(exprs: List[AST.Expr]): Result[Function] = args match
      case EmptyTuple if exprs.isEmpty => unsafeResult(EmptyTuple)
      case Type.VarArg(tpe) if exprs.forall(_.tpe <= tpe) => unsafeResult(exprs)
      case args: Tuple if {
            args.toList
              .zip(exprs)
              .forall:
                case (a: Type, AST.Expr(b)) => b <= a
                case _ => false
          } =>
        unsafeResult(exprs.toTuple)

      case _ =>
        Result.error(exprs.head.line)(Type.Function(args, resultHint), s"Invalid arguments: $exprs, expected: $args")

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Function(args, result) => this.resultHint <= resultHint
      case _ => false

  case class Vector(arity: scala.Int | Null = null) extends Type(arity != null):
    override def toString: Predef.String = arity match
      case null => "Vector[?]"
      case a => s"Vector[$a]"

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Vector(arity) => this.arity == arity
      case _ => false

  case class Matrix(rows: scala.Int | Null = null, cols: scala.Int | Null = null)
    extends Type(rows != null && cols != null) {
    override def toString: Predef.String = (rows, cols) match
      case (null, null) => "Matrix[?, ?]"
      case (null, b) => s"Matrix[?, $b]"
      case (a, null) => s"Matrix[$a, ?]"
      case (a, b) => s"Matrix[$a, $b]"

    def arity: (scala.Int | Null, scala.Int | Null) = (rows, cols)

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Matrix(rows, cols) if this.isFinal && other.isFinal => this.rows == rows && this.cols == cols
      case _: Type.Matrix => true
      case _ => false
  }

  case class VarArg[+T <: Type](tpe: T) extends Type(false):
    override def toString = s"$tpe*"

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.VarArg(tpe) => tpe <= this.tpe
      case _ => false

  case object Undef extends Type(false):
    override protected def isSubtype(other: Type): Boolean = false

  case object Unit extends Type:
    override protected def isSubtype(other: Type): Boolean = other == Unit

  case object Any extends Type(false):
    override protected def isSubtype(other: Type): Boolean = false

  case object Int extends Type:
    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Int => true
      case _ => false

  case object Float extends Type:
    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Float => true
      case _ => false

  case object String extends Type:
    override protected def isSubtype(other: Type): Boolean = other match
      case Type.String => true
      case _ => false

  case object Bool extends Type:
    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Bool => true
      case _ => false

  case object Range extends Type:
    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Range => true
      case _ => false

    override protected def isSubtype(other: Type): Boolean = other == Type.Range

extension [T](list: List[T])
  def toTuple: Tuple = list match
    case Nil => EmptyTuple
    case h :: t => h *: t.toTuple

val unary_numerical_type = Type.Function(Tuple(Type.Int), Type.Int) | Type.Function(Tuple(Type.Float), Type.Float)
val unary_vector_type = Type.OverloadedFunction(
  arg = Type.Vector(),
  resultHint = Type.Vector(),
  resultTypeFactory = expr => Result.Success(expr.tpe),
)
val unary_matrix_type = Type.OverloadedFunction(
  arg = Type.Matrix(),
  resultHint = Type.Matrix(),
  resultTypeFactory = expr => Result.Success(expr.tpe),
)

val binary_numerical_type = Type.Function((Type.Int, Type.Int), Type.Int) |
  Type.Function((Type.Numerical, Type.Numerical), Type.Float)

val binary_numerical_condition_type = Type.Function((Type.Numerical, Type.Numerical), Type.Bool)

val binary_matrix_type = Type.OverloadedFunction(
  args = (Type.Matrix(), Type.Matrix()),
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case (e @ AST.Expr(a: Type.Matrix), AST.Expr(b: Type.Matrix)) =>
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

      if errors.nonEmpty then Result.error(e.line)(Type.Matrix(rows, cols), errors.toSeq*)
      else if warns.nonEmpty then Result.warn(e.line)(Type.Matrix(rows, cols), warns.toSeq*)
      else Result.Success(Type.Matrix(rows, cols)),
)

val binary_vector_type = Type.OverloadedFunction(
  args = (Type.Vector(), Type.Vector()),
  resultHint = Type.Vector(),
  resultTypeFactory =
    case (e @ AST.Expr(a: Type.Vector), AST.Expr(b: Type.Vector)) =>
      if (a.arity == null) || (b.arity == null) then
        Result.warn(e.line)(Type.Matrix(), "Vector arity could not be inferred")
      else if a.arity != b.arity then
        Result.error(e.line)(Type.Matrix(), s"Vector lengths mismatch: ${a.arity} != ${b.arity}")
      else Result.Success(Type.Vector(a.arity)),
)

val scalar_type = Type.OverloadedFunction(
  args = (Type.Matrix(), Type.Numerical),
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case (expr, args) => Result.Success(expr.tpe),
) | Type.OverloadedFunction(
  args = (Type.Vector(), Type.Numerical),
  resultHint = Type.Vector(),
  resultTypeFactory =
    case (expr, args) => Result.Success(expr.tpe),
)

val matrix_type = Type.OverloadedFunction(
  arg = Type.Int,
  resultHint = Type.Matrix(),
  resultTypeFactory =
    case AST.Literal(Type.Int, n: Int, _) => Result.Success(Type.Matrix(n, n))
    case e @ AST.Literal(tpe, _, _) => Result.error(e.line)(Type.Matrix(), s"Matrix size must be an Int, got $tpe")
    case e => Result.warn(e.line)(Type.Matrix(), "Matrix size could not be inferred"),
)
