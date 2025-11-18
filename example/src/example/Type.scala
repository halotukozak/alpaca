package example

sealed trait Type(val isFinal: Boolean = true) {
  def |(other: Type): Type = Type.Or(this, other)

  def <=(other: Type): Boolean = (this, other) match
    case (_, Type.Any) => true
    case (Type.Any, _) => false
    case (left: Type.AnyOf, right: Type.AnyOf) => left.all.forall(l => right.all.exists(r => l <= r))
    case (left: Type.AnyOf, right) => left.all.exists(l => l <= right)
    case (left, right: Type.AnyOf) => right.all.exists(r => left <= r)
    case _ => this == other
}

//#
//# class AnyOf(Type):
//#     all: list[Type]
//#     is_final = False
//#
//#     def __init__(self, *types: Type):
//#         super().__init__()
//#         self.all = list(set(types))
//#
//#     def __repr__(self) -> str:
//#         return ' | '.join(map(str, self.all))
//#
//#     def __str__(self) -> str:
//#         return ' | '.join(map(str, self.all))
//#
//#     def __iter__(self) -> Iterator[Type]:
//#         return iter(self.all)
//#
//#

//#
//# @dataclass
//# class VarArg(Type):
//#     type: Type
//#
//#     def __str__(self) -> str:
//#         return f"({self.type})*"
//#
//#     def __repr__(self) -> str:
//#         return self.__str__()
//#
//#     def __hash__(self) -> int:
//#         return hash(repr(self))
//#
//#
//# class Function(Type):
//#     args: None | Type | Tuple[Type, ...] | VarArg
//#     arity: Optional[int]
//#     result: Type
//#
//#     def __init__(self, args: None | Type | Tuple[Type, ...] | VarArg, result: Type):
//#         if args is None:
//#             self.arity = 0
//#         elif isinstance(args, VarArg):
//#             self.arity = None
//#         elif isinstance(args, Type):
//#             self.arity = 1
//#         elif isinstance(args, Tuple):  # type: ignore
//#             self.arity = len(args)
//#         self.args = args
//#         self.result = result
//#
//#     def __str__(self) -> str:
//#         return f"({self.args}) -> {self.result}"
//#
//#     def __repr__(self) -> str:
//#         return self.__str__()
//#
//#     def __eq__(self, other: object) -> bool:
//#         return isinstance(other, Function) and self.args == other.args and self.result == other.result
//#
//#     def __hash__(self) -> int:
//#         return hash(repr(self))
//#
//#     def takes(self, args: list[Type]) -> bool:
//#         if self.args is None:
//#             return not args
//#         elif isinstance(self.args, VarArg):
//#             return all(a == self.args.type for a in args)
//#         elif isinstance(self.args, Type):
//#             return len(args) == 1 and self.args == args[0]
//#         elif isinstance(self.args, Tuple):  # type: ignore
//#             return len(self.args) == len(args) and all(a == b for a, b in zip(self.args, args))
//#         else:
//#             return False
//#
//#
//# class FunctionTypeFactory(Function):
//#     is_final = False
//#
//#     def __init__(self, args: None | Type | Tuple[Type, ...] | VarArg, result_hint: Type,
//#                  result_type_factory: Callable[..., Result[Type]]):
//#         super().__init__(args, result_hint)
//#         self.result_type_factory = result_type_factory
//#
//#     def __call__(self, args: list) -> Result[Type]:
//#         return self.result_type_factory(*args).map(lambda res: Function(self.args, res))
//#
//#     def __str__(self) -> str:
//#         return f"TypeFunction: [{hash(self.result_type_factory)}] => {self.args} -> {self.result}"  # todo: sth more informative
//#
//#     def __repr__(self) -> str:
//#         return self.__str__()
//#
//#     def __eq__(self, other: object) -> bool:
//#         return isinstance(other, FunctionTypeFactory) and self.result_type_factory == other.result_type_factory
//#
//#     def __hash__(self) -> int:
//#         return hash(repr(self))
//#
//#

object Type:
  type Numerical = Numerical.type
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

  def Or(left: Type, right: Type) = (left, right) match
    case (left: Any, _) => Any
    case (_, right: Any) => Any
    case (left: AnyOf, right: AnyOf) => AnyOf(left.all ++ right.all)
    case (left: AnyOf, right) => AnyOf(left.all + right)
    case (left, right: AnyOf) => AnyOf(right.all + left)
    case (left, right) if left == right => left
    case _ => AnyOf(Set(left, right))

  case class AnyOf(all: Set[Type]) extends Type(false)

  case class Function(args: Type*)(val result: Type) extends Type:
    def takes(argTypes: List[Type]) = args match
      case Nil => argTypes.isEmpty
      case Type.VarArg(tpe) :: Nil => argTypes.forall(_ == tpe)
      case `args` => true
      case _ => false

  case class FunctionTypeFactory(
    args: Tuple,
    result_hint: Type,
    result_type_factory: Tuple.Map[args.type, [X] =>> AST.Expr] => Result[Type],
//    result_type_factory: Tuple.Map[args.type, [X] =>> AST.Expr[X & Type]] => Result[Type],
  ) extends Type(false)

  case class Vector(arity: scala.Int | Null = null) extends Type(arity != null) {
    override def toString = arity match
      case null => "Vector[?]"
      case a => s"Vector[$a]"
  }

  case class Matrix(rows: scala.Int | Null = null, cols: scala.Int | Null = null)
    extends Type(rows != null && cols != null) {
    override def toString = (rows, cols) match
      case (null, null) => "Matrix[?, ?]"
      case (null, b) => s"Matrix[?, $b]"
      case (a, null) => s"Matrix[$a, ?]"
      case (a, b) => s"Matrix[$a, $b]"

    def arity = (rows, cols)
  }

//  case class VarArg[T <: Type](`type`: T) extends Type(false)
  case class VarArg(`type`: Type) extends Type(false)

  case object Undef extends Type(false)

  case object Unit extends Type

  case object Any extends Type(false)

  case object Int extends Type

  case object Float extends Type

  case object String extends Type

  case object Bool extends Type

  case object Range extends Type

  object FunctionTypeFactory:
//    def apply(arg: Type, result_hint: Type, result_type_factory: AST.Expr[arg.type] => Result[Type]) =
    def apply(arg: Type, result_hint: Type, result_type_factory: AST.Expr => Result[Type]) =
      new FunctionTypeFactory(
        Tuple(arg),
        result_hint,
        { case x *: EmptyTuple => result_type_factory(x) },
      )
    def varargs(
      arg: Type.VarArg,
      result_hint: Type,
      result_type_factory: Seq[AST.Expr] => Result[Type],
    ) = new FunctionTypeFactory(
      Tuple(arg),
      result_hint,
      { case (vararg: AST.Expr) *: EmptyTuple => result_type_factory(AST.Expr.traverse(vararg)) },
    )
