package example
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

  val Numerical = Int | Float
  type Numerical = Numerical.type

  def Or(left: Type, right: Type) = (left, right) match
    case (left: Any, _) => Any
    case (_, right: Any) => Any
    case (left: AnyOf, right: AnyOf) => AnyOf(left.all ++ right.all)
    case (left: AnyOf, right) => AnyOf(left.all + right)
    case (left, right: AnyOf) => AnyOf(right.all + left)
    case (left, right) if left == right => left
    case _ => AnyOf(Set(left, right))

  case class AnyOf(all: Set[Type]) extends Type(false):
    override def toString = all.mkString(" | ")
    override def <=(other: Type): Boolean = all.forall(_ <= other)
    override protected def isSubtype(other: Type): Boolean = false // Should not be called via <=

  case class Function(
    args: Tuple,
    result: Type,
  )(using args.type <:< TupleFactory[Tuple.Size[args.type], Type],
  ) extends Type:
    def takes(provided: List[Type]) = args match
      case _: EmptyTuple => provided.isEmpty
      case Type.VarArg(tpe) *: EmptyTuple => provided.forall(_ <= tpe)
      case args: Tuple =>
        args.toList
          .zip(provided)
          .forall:
            case (a: Type, b: Type) => b <= a
            case _ => false

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Function(args, result) =>
        def validArgs = this.args.toList
          .zip(args.toList)
          .forall:
            case (a: Type, b: Type) => a <= b
            case _ => false

        def validResult = this.result <= result
        validResult && validArgs
      case _ => false

  case class FunctionTypeFactory(
    args: Tuple,
    resultHint: Type,
    resultTypeFactory: TupleFactory[Tuple.Size[args.type], AST.Expr] => Result[Type],
  )(using ev: args.type <:< TupleFactory[Tuple.Size[args.type], Type],
  ) extends Type(false):
    override def toString =
      s"TypeFunction: [$resultTypeFactory] => $args -> $resultHint" // todo: sth more informative

    def apply(exprs: TupleFactory[Tuple.Size[args.type], AST.Expr]): Result[Function] =
      resultTypeFactory(exprs).map(res => Function(this.args, res))

    def apply(exprs: List[AST.Expr]): Result[Function] =
      exprs.toTuple match
        case tuple
            if tuple.size == args.size &&
              tuple.toList
                .zip(args.toList)
                .forall:
                  case (AST.Expr(tpe), arg: Type) => tpe <= arg
                  case _ => false
            =>
          apply(tuple.asInstanceOf[TupleFactory[Tuple.Size[args.type], AST.Expr]])
        case _ => Result.error(Type.Function(args, resultHint), s"Invalid arguments: $exprs")

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Function(args, result) => this.resultHint <= resultHint
      case _ => false

  case class Vector(arity: scala.Int | Null = null) extends Type(arity != null):
    override def toString = arity match
      case null => "Vector[?]"
      case a => s"Vector[$a]"

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Vector(arity) => this.arity == arity
      case _ => false

  case class Matrix(rows: scala.Int | Null = null, cols: scala.Int | Null = null)
    extends Type(rows != null && cols != null) {
    override def toString = (rows, cols) match
      case (null, null) => "Matrix[?, ?]"
      case (null, b) => s"Matrix[?, $b]"
      case (a, null) => s"Matrix[$a, ?]"
      case (a, b) => s"Matrix[$a, $b]"

    def arity = (rows, cols)

    override protected def isSubtype(other: Type): Boolean = other match
      case Type.Matrix(rows, cols) => this.rows == rows && this.cols == cols
      case _ => false
  }

//  case class VarArg[T <: Type](`type`: T) extends Type(false)
  case class VarArg(tpe: Type) extends Type(false):
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

  object FunctionTypeFactory:
    def apply(arg: Type, resultHint: Type, resultTypeFactory: AST.Expr => Result[Type]) =
      new FunctionTypeFactory(
        Tuple(arg),
        resultHint,
        { case tpe *: EmptyTuple => resultTypeFactory(tpe) },
      )
    def varargs(
      arg: Type.VarArg,
      resultHint: Type,
      resultTypeFactory: Seq[AST.Expr] => Result[Type],
    ) = new FunctionTypeFactory(
      Tuple(arg),
      resultHint,
      { case vararg *: EmptyTuple => resultTypeFactory(AST.Expr.traverse(vararg)) },
    )

type TupleFactory[N <: scala.Int, X] = N match
  case 0 => EmptyTuple
  case S[n] => X *: TupleFactory[n, X]

extension [T](list: List[T])
  def toTuple: Tuple = list match
    case Nil => EmptyTuple
    case h :: t => h *: t.toTuple
