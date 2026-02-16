# Tutorial: Understanding Extractors

Alpaca leverages Scala 3's powerful pattern matching system to provide a type-safe and intuitive way to define grammar rules.
"Extractors" are the mechanism used to match terminals (tokens) and non-terminals (rules) within a `rule` block.

## 1. Matching Terminals (Tokens)

When you define a lexer, Alpaca automatically generates extractors for each token.
You can use these extractors to match tokens and access their values.

### Basic Token Matching
To match a token without caring about its value, use `_`:
```scala
case (MyLexer.PLUS(_), Expr(e)) => ...
```

### Accessing Token Values
To access the value captured by the token, bind it to a variable:
```scala
case MyLexer.NUM(n) => n.value // n is a Lexem object
```
The `Lexem` object contains:
- `value`: The extracted value (e.g., `Double`, `Int`, `String`).
- `name`: The name of the token.
- `fields`: A NamedTuple containing context information (like `line` and `position`).

### Accessing Context Fields
You can match directly on context fields if they are available in your lexer context. If such fields are not defined in your custom context, your code will not compile, ensuring type safety.
```scala
case MyLexer.NUM(n) => println(s"Found number on line ${n.line}")
```

## 2. Matching Non-Terminals (Rules)

Rules defined with `val myRule = rule(...)` also act as extractors.
When matched, they return the value produced by that rule.

```scala
val Expr: Rule[Int] = rule(...)
val Stmt: Rule[Unit] = rule:
  case Expr(e) => println(s"Expression result (Int): $e")
```

## 3. EBNF Extractors

Alpaca provides special extractors for common EBNF patterns: `List` and `Option`.

### The `.List` Extractor
Matches zero or more occurrences of a symbol. It returns a `List[T]`.

```scala
val Block: Rule[List[Stmt]] = rule:
  case Stmt.List(stmts) => stmts
```

### The `.Option` Extractor
Matches zero or one occurrence of a symbol. It returns an `Option[T]`.

```scala
val Decl: Rule[Val] = rule:
  case (MyLexer.VAL(_), MyLexer.ID(id), MyLexer.Type.Option(t) => ...
```

## 4. Tuple Matching

For sequences of symbols, use tuples:

```scala
case (MyLexer.IF(_), Expr(cond), MyLexer.THEN(_), Stmt(s)) => ...
```

## How it Works (Internal)

Alpaca's macros analyze these pattern matches at compile-time to:
1. Identify the symbols (terminals and non-terminals) involved.
2. Construct the grammar's production rules.
3. Generate the parse table.
4. Ensure type safety by verifying that the bound variables match the types produced by the rules/tokens.

By using standard Scala pattern matching, Alpaca makes defining complex grammars feel like writing regular Scala code while providing all the benefits of a formal parser generator.
