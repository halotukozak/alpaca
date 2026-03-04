# Tutorial: Understanding Extractors

Alpaca leverages Scala 3's powerful pattern matching system to provide a type-safe and intuitive way to define grammar
rules. "Extractors" are the mechanism used to match terminals (tokens) and non-terminals (rules) within a `rule` block.

## 1. Matching Terminals (Tokens)

When you define a lexer, Alpaca automatically generates extractors for each token. You can use these extractors to match
tokens and access their values.

### Basic Token Matching

To match a token without caring about its value, use `_`:

[//]: # (@formatter:off)
```scala
case (MyLexer.PLUS(_), Expr(e)) => ...
```
[//]: # (@formatter:on)

### Accessing Token Values

To access the value captured by the token, bind it to a variable:
[//]: # (@formatter:off)
```scala
case MyLexer.NUM(n) => n.value // n is a Lexeme object
```
[//]: # (@formatter:on)

The `Lexeme` object contains:

- `value`: The extracted value (e.g., `Double`, `Int`, `String`).
- `name`: The name of the token.
- `fields`: A NamedTuple containing context information (like `line` and `position`).

### Accessing Context Fields

You can match directly on context fields if they are available in your lexer context:
[//]: # (@formatter:off)
```scala
case MyLexer.NUM(n @ { line = l }) => println(s"Found number on line $l")
```
[//]: # (@formatter:on)

## 2. Matching Non-Terminals (Rules)

Rules defined with `val myRule = rule(...)` also act as extractors. When matched, they return the value produced by that
rule.

```scala
val Expr: Rule[Int] = rule(...)
val Stmt: Rule[Unit] = rule:
  case Expr(e) => println(s"Expression result: $e")
```

## 3. EBNF Extractors

Alpaca provides special extractors for common EBNF patterns: `List` and `Option`.
These work on **Rule** references (non-terminals), not on token references directly.

### The `.List` Extractor

Matches zero or more occurrences of a rule. It returns a `List[T]`.

```scala
val Stmt: Rule[String] = rule(...)

val Block: Rule[List[String]] = rule:
  case Stmt.List(stmts) => stmts
```

### The `.Option` Extractor

Matches zero or one occurrence of a rule. It returns an `Option[T]`.

```scala
val Num: Rule[Int] = rule:
  case MyLexer.NUM(n) => n.value

val Args: Rule[(Int, Option[Int])] = rule:
  case (Num(a), MyLexer.COMMA(_), Num.Option(b)) => (a, b)
```

## 4. Tuple Matching

For sequences of symbols, use tuples:
[//]: # (@formatter:off)
```scala
case (MyLexer.IF(_), Expr(cond), MyLexer.THEN(_), Stmt(s)) =>...
```
[//]: # (@formatter:on)

## How it Works (Internal)

Alpaca's macros analyze these pattern matches at compile-time to:

1. Identify the symbols (terminals and non-terminals) involved.
2. Construct the grammar's production rules.
3. Generate the parse table.
4. Ensure type safety by verifying that the bound variables match the types produced by the rules/tokens.

By using standard Scala pattern matching, Alpaca makes defining complex grammars feel like writing regular Scala code
while providing all the benefits of a formal parser generator.
