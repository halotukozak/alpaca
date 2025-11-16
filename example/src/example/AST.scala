package example

// +- Tree -+- Statement -+
//          +- Block      |
//                        +- Expr --------+- Literal
//                        |               +- Apply
//                        |               +- Number
//                        |               +- String
//                        |               +- Range
//                        |               +- Ref --------+- VectorRef
//                        |                              +- MatrixRef
//                        |                              +- SymbolRef
//                        +- Assign
//                        +- If
//                        +- While
//                        +- For
//                        +- Return
//                        +- Continue
//                        +- Break

object AST {
  sealed trait Tree:
    def line: Int | Null
  sealed trait Statement extends Tree

//class Tree:
//    def __init__(self, line: Optional[int]):
//        self.line = line or -1
//
//    # for mypy
//    def print_tree(self, indent_level: int) -> None:
//        pass
//
//
//

  case class Block(statements: List[Statement], line: Int | Null = null) extends Tree

//# synthetic tree node to represent a block of statements
//@dataclass(init=False)
//class Block(Tree):
//    statements: list[Statement]
//
//    def __init__(self, statements: list[Statement], line: Optional[int] = None):
//        super().__init__(line)
//        self.statements = statements
//
//    def __iter__(self) -> Iterator[Statement]:
//        return iter(self.statements)
//
//
//T = TypeVar('T', bound=TS.Type)
//
//

  sealed trait Expr[+T <: Type] extends Statement {
    def `type`: T | Null = null
  }

//class Expr[T](Statement):
//    def __init__(self, type_: Optional[TS.Type], line: Optional[int]):
//        super().__init__(line)
//        self.type = type_ or TS.undef()
//
//

  case class Literal[+T <: Type](override val `type`: T, value: Any, line: Int) extends Expr[T]

  // remove facoty methods
  object Literal {
    def float(value: Double, line: Int): Literal[Type.Float] =
      Literal(Type.Float, value, line)

    def string(value: String, line: Int): Literal[Type.String] =
      Literal(Type.String, value, line)

    def int(value: Int, line: Int): Literal[Type.Int] =
      Literal(Type.Int, value, line)
  }

//@dataclass(init=False)
//class Literal[T](Expr[T]):
//    value: Any
//
//    def __init__(self, value: Any, line: int, type_: TS.Type):
//        super().__init__(type_, line)
//        self.value = value
//
//    @staticmethod
//    def float(value: float, line: int) -> 'Literal[TS.Float]':
//        return Literal(float(value), line, TS.Float())
//
//    @staticmethod
//    def string(value: str, line: int) -> 'Literal[TS.String]':
//        return Literal(str(value), line, TS.String())
//
//    @staticmethod
//    def int(value: int, line: int) -> 'Literal[TS.Int]':
//        return Literal(int(value), line, TS.Int())
//
//
//@dataclass(init=False)
//class Ref[T](Expr[T]):
//    pass
//

  sealed trait Ref[+T <: Type] extends Expr[T]

  case class SymbolRef[+T <: Type](name: String, override val `type`: T, line: Int) extends Ref[T]

  case class VectorRef(vector: SymbolRef[?], element: Expr[Type.Int], line: Int) extends Ref[Type.Numerical]

//
//@dataclass(init=False)
//class VectorRef(Ref[TS.Int | TS.Float]):
//    vector: SymbolRef[TS.Vector]
//    element: Expr[TS.Int]
//
//    def __init__(self, vector: SymbolRef, element: Expr[TS.Int], line: int):
//        super().__init__(TS.Int() | TS.Float(), line)
//        self.vector = vector
//        self.element = element
//
//
//@dataclass(init=False)
//class MatrixRef(Ref[TS.Int | TS.Float]):
//    matrix: SymbolRef[TS.Matrix]
//
//    def __init__(self, matrix: SymbolRef, row: Optional[Expr[TS.Int]], col: Optional[Expr[TS.Int]], line: int):
//        super().__init__(TS.Int() | TS.Float(), line)
//        self.matrix = matrix
//        self.row = row
//        self.col = col
//
  case class MatrixRef(matrix: SymbolRef[?], row: Expr[Type.Int] | Null, col: Expr[Type.Int] | Null, line: Int)
    extends Ref[Type.Numerical]
//
//@dataclass(init=False)
//class Apply(Expr):
//    ref: Ref
//    args: list[Expr]
//
//    def __init__(self, ref: Ref, args: list[Expr], line: int):
//        super().__init__(TS.undef(), line)
//        self.ref = ref
//        self.args = args
//

  case class Apply(ref: Ref[?], args: List[Expr[?]], line: Int) extends Expr[Type.Undef]
//
//@dataclass
//class Range(Expr):
//    start: Expr[TS.Int]
//    end: Expr[TS.Int]
//    line: int
//

  case class Range(start: Expr[Type.Int], end: Expr[Type.Int], line: Int | Null) extends Expr[Type.Range]
//
//@dataclass
//class Assign[T](Statement):
//    var: Ref[T]
//    expr: Expr[T]
//    line: int
//
//

  case class Assign[+T <: Type](varRef: Ref[T], expr: Expr[T], line: Int) extends Statement

  case class If(condition: Expr[Type.Bool], thenBlock: Block, elseBlock: Block | Null, line: Int) extends Statement

//@dataclass(init=False)
//class If(Statement):
//    condition: Expr[TS.Bool]
//    then: Block
//    else_: Optional[Block]
//
//    def __init__(self, condition: Expr[TS.Bool], then: list[Statement], else_: Optional[list[Statement]], line: int):
//        super().__init__(line)
//        self.condition = condition
//        self.then = Block(then)
//        self.else_ = Block(else_) if else_ else None
//
//
  case class While(condition: Expr[Type.Bool], body: Block, line: Int) extends Statement

//@dataclass(init=False)
//class While(Statement):
//    condition: Expr
//    body: Block
//
//    def __init__(self, condition: Expr, body: list[Statement], line: int):
//        super().__init__(line)
//        self.condition = condition
//        self.body = Block(body)
//        self.line = line
//
//
//@dataclass(init=False)
//class For(Statement):
//    var: SymbolRef
//    range: Range
//    body: Block
//
//    def __init__(self, var: SymbolRef, range_: Range, body: list[Statement], line: int):
//        super().__init__(line)
//        self.var = var
//        self.range = range_
//        self.body = Block(body)
//
//

  case class For(varRef: SymbolRef[Type.Int], range: Range, body: Block, line: Int) extends Statement

  case class Return[+T <: Type](expr: Expr[T], line: Int) extends Statement

  case class Continue(line: Int) extends Statement

  case class Break(line: Int) extends Statement
}

enum Comparator:
  case Greater
  case Less
  case Equal
  case NotEqual
  case LessEqual
  case GreaterEqual
