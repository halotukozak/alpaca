# Tutorial: Understanding Extractors

Extractors are how you match terminals (tokens) and non-terminals (rules) within `rule` blocks. Alpaca generates them
automatically from your lexer and parser definitions.

## 1. Matching Terminals (Tokens)

Match a token without its value using `_`, or bind it to access the `Lexeme`:

[//]: # (@formatter:off)
```scala
case (MyLexer.PLUS(_), Expr(e)) => ...     // ignore token value
case MyLexer.NUM(n) => n.value              // n is a Lexeme
```
[//]: # (@formatter:on)

The `Lexeme` object contains `value`, `name`, and `fields` (a NamedTuple with context like `line` and `position`).

You can match directly on context fields:

[//]: # (@formatter:off)
```scala
case MyLexer.NUM(n @ { line = l }) => println(s"Found number on line $l")
```
[//]: # (@formatter:on)

## 2. Matching Non-Terminals (Rules)

Rules also act as extractors, returning the value produced by that rule:

```scala
val Expr: Rule[Int] = rule(...)
val Stmt: Rule[Unit] = rule:
  case Expr(e) => println(s"Expression result: $e")
```

## 3. EBNF Extractors

`.List` and `.Option` work on **Rule** references (not tokens) for common EBNF patterns:

```scala
// .List — zero or more occurrences, returns List[T]
val Block: Rule[List[String]] = rule:
  case Stmt.List(stmts) => stmts

// .Option — zero or one occurrence, returns Option[T]
val Args: Rule[(Int, Option[Int])] = rule:
  case (Num(a), MyLexer.COMMA(_), Num.Option(b)) => (a, b)
```

## 4. Tuple Matching

For sequences of symbols, use tuples:

[//]: # (@formatter:off)
```scala
case (MyLexer.IF(_), Expr(cond), MyLexer.THEN(_), Stmt(s)) => ...
```
[//]: # (@formatter:on)
