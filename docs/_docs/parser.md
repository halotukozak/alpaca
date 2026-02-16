# Parser

The Parser in Alpaca transforms a stream of tokens (lexemes) into a structured result, such as an Abstract Syntax Tree (AST) or a direct evaluation result. It uses a type-safe DSL to define Context-Free Grammars (CFG) and uses an LR parsing algorithm.

## Defining a Parser

To define a parser, create an object that extends the `Parser` class. You define grammar rules using the `rule` function.

```scala
import alpaca.*

object MyParser extends Parser:
  // The root rule is the entry point for parsing
  val root: Rule[Double] = rule:
    case Expr(e) => e

  val Expr: Rule[Double] = rule(
    { case (Expr(l), MyLexer.PLUS(_), Term(r)) => l + r },
    { case Term(t) => t }
  )

  val Term: Rule[Double] = rule(
    { case (Term(l), MyLexer.STAR(_), Factor(r)) => l * r },
    { case Factor(f) => f }
  )

  val Factor: Rule[Double] = rule(
    { case MyLexer.NUM(n) => n.value },
    { case (MyLexer.LP(_), Expr(e), MyLexer.RP(_)) => e }
  )
```

### Rules and Productions

- **Rule**: A non-terminal symbol in your grammar.
- **Production**: A single alternative for a rule, defined as a partial function.
- **Pattern Matching**: Productions use pattern matching to specify the sequence of symbols. You can match:
    - **Terminals**: Tokens from your lexer (e.g., `MyLexer.PLUS(_)`).
    - **Non-Terminals**: Other rules (e.g., `Expr(e)`).
    - **Tuples**: A sequence of symbols (e.g., `(Expr(l), MyLexer.PLUS(_), Term(r))`).

## EBNF Operators

Alpaca provides built-in support for common EBNF patterns:

### Optional

Use `.Option` to match a rule zero or one time. It returns an `Option[T]`.

```scala
val Decl: Rule[ValDecl] = rule:
  case (MyLexer.VAL(_), MyLexer.ID(id), MyLexer.COLON(_).Option, Type(t)) => 
    ValDecl(id.value, t)
```

### Repeated

Use `.List` to match a rule zero or more times. It returns a `List[T]`.

```scala
val Block: Rule[List[Stmt]] = rule:
  case Stmt.List(stmts) => stmts
```

## Conflict Resolution

LR parsers can have conflicts (Shift/Reduce or Reduce/Reduce). Alpaca allows you to resolve these by specifying precedence and associativity.

### Precedence Rules

Override the `resolutions` member in your parser:

```scala
object MyParser extends Parser:
  // ... rules ...

  override val resolutions = Set(
    MyLexer.STAR after MyLexer.PLUS,
    MyLexer.SLASH after MyLexer.PLUS,
    // ...
  )
```

### Named Productions

For more fine-grained control, you can name your productions:

```scala
  val Expr: Rule[Int] = rule(
    "add" { case (Expr(l), MyLexer.PLUS(_), Expr(r)) => l + r },
    "mul" { case (Expr(l), MyLexer.STAR(_), Expr(r)) => l * r },
    { case MyLexer.NUM(n) => n.value }
  )

  override val resolutions = Set(
    Production("mul") after Production("add")
  )
```

## Parser Context

Similar to the lexer, the parser can use a `ParserCtx` to maintain global state.

```scala
case class MyParserCtx(var symbolCount: Int = 0) extends ParserCtx

object MyParser extends Parser[MyParserCtx]:
  val root = rule:
    case MyLexer.ID(id) => 
      ctx.symbolCount += 1
      id.value
```

## Parsing Input

To parse a list of lexemes, call the `parse` method:

```scala
val lexemes = myLexer.tokenize("1 + 2")._2
val (finalCtx, result) = MyParser.parse(lexemes)
```

## Internal Working

At compile-time, the `Parser` macro:
1. Analyzes the grammar rules and builds a collection of LR items.
2. Computes the First and Follow sets.
3. Generates the LR(1) (or LALR) transition and action tables.
4. Validates the grammar for conflicts and reports unresolved ones.
5. Generates optimized Scala code for the parser state machine.
