# Parser

ALPACA generates highly efficient, canonical LR(1) parsers from a declarative grammar specification.

## Defining a Parser

To define a parser, create an object that extends `Parse`. Use the `rule` macro to define grammar rules.

```scala
import alpaca.*

object MyParser extends Parser {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), MyLexer.PLUS(_), Expr(b)) => a + b },
    { case (Expr(a), MyLexer.MINUS(_), Expr(b)) => a - b },
    { case MyLexer.NUM(n) => n.value }
  )

  val root = rule { case Expr(e) => e }
}
```

### Grammar Rules

- **Productions**: Each rule consists of one or more productions, defined as partial functions.
- **Root Rule**: Every parser must define a `root` rule, which is the starting point for parsing.
- **Typed Rules**: Each `Rule[T]` produces a value of type `T` when matched.

## Pattern Matching in Productions

Productions use Scala's pattern matching to describe the right-hand side of a grammar rule.

- **Tokens**: Reference tokens from your lexer (e.g., `MyLexer.ID(id)`).
- **Rules**: Reference other rules as extractors (e.g., `Expr(e)`).

## EBNF-like Syntax

ALPACA provides built-in support for common grammar patterns:

### Optional Symbols (`.Option`)
Matches zero or one occurrence of a rule. Returns an `Option[T]`.

```scala
val Stmt: Rule[Unit] = rule(
  { case (Expr(e), MyLexer.SEMICOLON.Option(_)) => println(e) }
)
```

### Lists of Symbols (`.List`)
Matches zero or more occurrences of a rule. Returns a `List[T]`.

```scala
val Block: Rule[List[Int]] = rule(
  { case (MyLexer.LBRACE(_), Expr.List(exprs), MyLexer.RBRACE(_)) => exprs }
)
```

## Conflict Resolution

LR(1) parsers can have shift/reduce or reduce/reduce conflicts when the grammar is ambiguous (like in arithmetic expressions). ALPACA allows you to resolve these using precedence rules in the `resolutions` set.

```scala
object MyParser extends Parser {
  // ... rules ...

  override val resolutions = Set(
    Production(Expr, MyLexer.TIMES, Expr).before(MyLexer.PLUS, MyLexer.MINUS),
    Production(Expr, MyLexer.DIVIDE, Expr).before(MyLexer.PLUS, MyLexer.MINUS),
    Production(MyLexer.MINUS, Expr).before(MyLexer.TIMES, MyLexer.DIVIDE)
  )
}
```

- **`before`**: Specifies that the production should have higher precedence than the listed tokens or other productions.
- **`after`**: Specifies that the production should have lower precedence.

You can refer to a production by listing its right-hand side symbols in `Production(...)`, or by name if you used the `@name` annotation:

```scala
{ case (Expr(a), MyLexer.PLUS(_), Expr(b)) => a + b }: @name("plus")

// In resolutions:
Production.ofName("plus").after(MyLexer.TIMES)
```

## Semantic Actions and Context

The body of each production's partial function is a semantic action. You can use it to build AST nodes, perform calculations, or update the parser context.

```scala
case class CalcCtx(var operations: Int = 0) extends ParserCtx

object CalcParser extends Parser[CalcCtx] {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), MyLexer.PLUS(_), Expr(b)) => 
        ctx.operations += 1
        a + b 
    }
    // ...
  )
  // ...
}
```

The `ctx` variable provides access to the `ParserCtx` instance.

## Usage

To run the parser, pass the lexemes from the lexer to the `parse` method:

```scala
val lexemes = myLexer.tokenize("1 + 2 * 3").lexemes
val (finalCtx, result) = MyParser.parse(lexemes)

println(s"Result: $result")
```
