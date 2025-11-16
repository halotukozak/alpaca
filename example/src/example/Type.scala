package example

sealed trait Type(val isFinal: Boolean = true) {
  type Scala
}

//#
//# class Type:
//#     is_final: bool = True
//#
//#     def __eq__(self, other: object) -> bool:
//#         if isinstance(other, Any) or isinstance(self, Any):
//#             return True
//#         elif isinstance(other, AnyOf) and isinstance(self, AnyOf):
//#             return set(self.all).intersection(other.all) != set()
//#         elif isinstance(self, AnyOf) and isinstance(other, Type):
//#             return other in self
//#         elif isinstance(other, AnyOf):
//#             return self in other
//#         else:
//#             return type(self) == type(other)
//#
//#     def __str__(self) -> str:
//#         return type(self).__name__
//#
//#     def __repr__(self) -> str:
//#         return type(self).__name__
//#
//#     def __or__(self, other: 'Type') -> 'Type':
//#         if isinstance(other, Any) or isinstance(self, Any):
//#             return Any()
//#         elif isinstance(other, AnyOf) and isinstance(self, AnyOf):
//#             return AnyOf(*self.all, *other.all)
//#         elif isinstance(self, AnyOf):
//#             return AnyOf(*self.all, other)
//#         elif isinstance(other, AnyOf):
//#             return AnyOf(self, *other.all)
//#         elif self == other:
//#             return self
//#         else:
//#             return Or(self, other)
//#
//#     def __hash__(self) -> int:
//#         return hash(repr(self))
//#
//#
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

//# class Or(AnyOf):
//#     def __init__(self, left: Type, right: Type):
//#         super().__init__(left, right)
//#
//#
//# # arity has no meaning in equality!
//# # todo: vector should be a metrix with 1 column/row
//# class Vector(Type):
//#     def __init__(self, arity: Optional[int] = None):
//#         self.arity = arity
//#         if arity is None:
//#             self.is_final = False
//#
//#     def __str__(self) -> str:
//#         if self.arity is not None:
//#             return f"Vector[{self.arity}]"
//#         else:
//#             return f"Vector[?]"
//#
//#     def __repr__(self) -> str:
//#         return self.__str__()
//#
//#
//# # arity has no meaning in equality!
//# class Matrix(Type):
//#     def __init__(self, rows: Optional[int] = None, cols: Optional[int] = None):
//#         self.rows = rows
//#         self.cols = cols
//#         if not self.rows or not self.cols:
//#             self.is_final = False
//#
//#     def __str__(self) -> str:
//#         match self.arity:
//#             case (None, None):
//#                 return "Matrix[?, ?]"
//#             case (None, b):
//#                 return f"Matrix[?, {b}]"
//#             case (a, None):
//#                 return f"Matrix[{a}, ?]"
//#             case (a, b):
//#                 return f"Matrix[{a}, {b}]"
//#             case _:
//#                 raise NotImplementedError
//#
//#     def __repr__(self) -> str:
//#         return self.__str__()
//#
//#     @property
//#     def arity(self) -> Tuple[Optional[int], Optional[int]]:
//#         return self.rows, self.cols
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
//# class Int(Type):
//#     pass
//#
//#
//# class Float(Type):
//#     pass
//#
//#
//# class String(Type):
//#     pass
//#
//#
//# class Bool(Type):
//#     pass
//#
//#
//# def numerical() -> Type:
//#     return Int() | Float()

object Type:
  type Numerical = Int | Float
  type Undef = Undef.type
  type Unit = Unit.type
  type Any = Any.type
  type Int = Int.type
  type Float = Float.type
  type String = String.type
  type Bool = Bool.type
  type Range = Range.type

  def Or(left: Type, right: Type) = AnyOf(Set(left, right))

  case class AnyOf(all: Set[Type]) extends Type(false)

  case object Undef extends Type(false)

  case object Unit extends Type

  case object Any extends Type(false)

  case object Int extends Type

  case object Float extends Type

  case object String extends Type

  case object Bool extends Type

  case object Range extends Type

  type FromScala[T] = T match
    case scala.Int => Type.Int
    case scala.Float => Type.Float
    case scala.Unit => Type.Unit
    case scala.Boolean => Type.Bool
    case scala.Predef.String => Type.String

  inline def from[T]: Type.FromScala[T] =
    inline compiletime.erasedValue[T] match
      case _: scala.Int => Type.Int
      case _: scala.Float => Type.Float
      case _: scala.Unit => Type.Unit
      case _: scala.Boolean => Type.Bool
      case _: scala.Predef.String => Type.String
