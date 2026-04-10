# Extractors

Parser rule bodies are partial functions — everything on the left side of `=>` is a pattern.
Extractors provide type-safe access to terminals (tokens), non-terminals (rule results), and EBNF operators.
This page covers every extractor form available in parser rules.

> **Compile-time processing:** The Alpaca macro transforms `case CalcLexer.NUMBER(n) =>` into a pattern that extracts a `Lexeme` from the parse stack. The extractor logic is wired at compile time — what you write in case patterns is syntactic sugar that the macro resolves against the grammar.

## Terminal Extractors

Use `MyLexer.TOKEN(binding)` to match a terminal in a case pattern.
The `binding` variable receives a `Lexeme` — **not** the extracted value.
Use `binding.value` to access the semantic content.

For structural tokens (operators, punctuation) where the value is not needed, use `_` to discard the binding.
Token names that are not valid Scala identifiers (containing `+`, `(`, `)`, reserved words like `if`) must be quoted with backticks.

```scala sc:nocompile
import alpaca.*

// Value-bearing token: use binding.value for the semantic content
{ case CalcLexer.NUMBER(n) => n.value }     // n: Lexeme, n.value: Double

// Structural token: discard the binding when the value is not needed
{ case CalcLexer.PLUS(_) => () }

// Backtick quoting for special-character token names
{ case CalcLexer.`\\+`(_) => () }
{ case (CalcLexer.`\\(`(_), Expr(e), CalcLexer.`\\)`(_)) => e }
```

**Pitfall:** After `CalcLexer.NUMBER(n)`, the variable `n` is a `Lexeme`, not an `Int`.
Using `n` where a `Double` is expected is a type error. Always use `n.value` for the semantic content.

## Non-Terminal Extractors

Use `Rule(binding)` in a case pattern to match a non-terminal.
This calls `Rule[R].unapply`, extracting the value of type `R` produced by that rule during the parse.
Rules can refer to themselves recursively — the macro handles left recursion and mutual recursion automatically.

```scala sc:nocompile
import alpaca.*

// Expr(left) extracts the Int produced by the Expr rule
{ case (Expr(left), CalcLexer.PLUS(_), Expr(right)) => left + right }
// left: Int, right: Int  (from Rule[Int])

// Single non-terminal: direct match, no wrapper
{ case Expr(e) => e }
```

A non-terminal extractor is available for any `Rule[R]` defined in the parser.
The binding variable has exactly type `R` — no cast or conversion needed.
If two rules produce different types, the types appear naturally in the pattern:

```scala sc:nocompile
import alpaca.*

val Name:  Rule[String] = rule:
  case CalcLexer.ID(id) => id.value

val Value: Rule[Double] = rule:
  case CalcLexer.NUMBER(n) => n.value

val root = rule:
  case (Name(key), CalcLexer.ASSIGN(_), Value(v)) => (key, v)
  // key: String (from Rule[String]), v: Double (from Rule[Double])
```

## Tuple Patterns (Multi-Symbol Productions)

The way a case pattern is written depends on how many symbols the production contains:

- **Multi-symbol productions** match a **tuple**: `{ case (sym1, sym2, sym3) => ... }`
- **Single-symbol productions** match **directly**: `{ case sym1 => ... }` — not wrapped in a tuple

This is a Scala pattern matching requirement, not an Alpaca-specific rule.
Writing a tuple pattern with only one element, or omitting the parentheses for multiple elements, is a compile error.

```scala sc:nocompile
import alpaca.*

val Expr: Rule[Int] = rule(
  // Multi-symbol: parentheses required
  { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
  // Single-symbol: no parentheses
  { case CalcLexer.NUMBER(n) => n.value },
)
```

## EBNF Extractors: .Option

`Rule.Option(binding)` in a case pattern binds `binding` to an `Option[R]`.
The macro generates two synthetic productions at compile time: an empty production (returns `None`) and a single-element production (returns `Some`).

```scala sc:nocompile
import alpaca.*

val Num: Rule[Int] = rule:
  case CalcLexer.NUMBER(n) => n.value

val root = rule:
  case (CalcLexer.LPAREN(_), Num.Option(maybeNum), CalcLexer.RPAREN(_)) =>
    maybeNum    // Option[Int] — None if absent, Some(n) if present
```

## EBNF Extractors: .List

`Rule.List(binding)` in a case pattern binds `binding` to a `List[R]`.
The macro generates a left-recursive accumulation: an empty production (returns `Nil`) and an appending production (returns `list :+ elem`).

```scala sc:nocompile
import alpaca.*

val Num: Rule[Int] = rule:
  case CalcLexer.NUMBER(n) => n.value

val root = rule:
  case Num.List(numbers) =>
    numbers    // List[Int] — zero or more Num values
```

`.Option` and `.List` also work on terminals (from `DefinedToken`), not only rules:

```scala sc:nocompile
// Token-level EBNF: zero or more NUMBER lexemes
val root = rule:
  case CalcLexer.NUMBER.List(numbers) =>
    numbers    // List[Lexeme] — zero or more NUMBER lexemes
```

## Combining EBNF in One Production

`.Option` and `.List` can appear together in the same tuple pattern:

```scala sc:nocompile
import alpaca.*

val Num: Rule[Int] = rule:
  case CalcLexer.NUMBER(n) => n.value

val root = rule:
  case (Num(n), CalcLexer.COMMA(_), Num.Option(opt), CalcLexer.COMMA(_), Num.List(lst)) =>
    (n, opt, lst)
    // n: Int, opt: Option[Int], lst: List[Int]

// "1,,3"       => (1, None, List(3))
// "1,2,1 2 3"  => (1, Some(2), List(1, 2, 3))
```

## Lexeme Object Structure

When a terminal extractor binds a variable, the variable is a `Lexeme`.
A `Lexeme` is the record that crosses the lexer-to-parser boundary and carries both the extracted value and a snapshot of the lexer context at match time.

The user-visible fields are:

- **`name: String`** — the token type name (e.g., `"NUMBER"`, `"PLUS"`)
- **`value: T`** — the extracted value; the type depends on the `Token["NAME"](value)` definition in the lexer
- **`text: String`** — the raw matched characters; always a `String` regardless of token type
- **`position: Int`** — character position at match time (post-match; incremented by token length before the snapshot)
- **`line: Int`** — line number at match time
- **`fields: Map[String, Any]`** — all context fields at match time, accessible by name

`Lexeme` extends `Selectable`, so field access is type-safe at compile time — `id.position` returns `Int`, not `Any`.
The type refinement is encoded in the `tokenize()` return type and flows through to the parser.

A concrete example of the snapshot embedded in each lexeme:

```scala sc:nocompile
import alpaca.*

val MiniLang = lexer:
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+"          => Token["PLUS"]
  case "\\s+"         => Token.Ignored

val (_, lexemes) = MiniLang.tokenize("42 + 13")
// lexemes(0): Lexeme("NUM",  42, Map("text" -> "42", "position" -> 3,  "line" -> 1))
// lexemes(1): Lexeme("PLUS", (), Map("text" -> "+",  "position" -> 5,  "line" -> 1))
// lexemes(2): Lexeme("NUM",  13, Map("text" -> "13", "position" -> 8,  "line" -> 1))

// Inside parser rules, access via dot notation:
//   n.value     == 42     (Int)
//   n.text      == "42"   (String — matched characters)
//   n.position  == 3      (Int — post-match character position)
//   n.line      == 1      (Int — line number)
```

Available fields depend on the `LexerCtx` used to build the lexer:
- `LexerCtx.Default` provides `text`, `position`, and `line`.
- Adding `LineTracking` (already included in `LexerCtx.Default`) provides `line`.
- Custom context fields appear if the lexer context declares them.

See [Between Stages](between-stages.html) for the full Lexeme structure, context snapshot lifecycle, and how positional values are computed.

## Accessing Fields on a Bound Lexeme

After binding a terminal, use dot notation to access any field from the context snapshot:

```scala sc:nocompile
import alpaca.*

{ case CalcLexer.ID(id) =>
    val name = id.value      // String — the identifier text
    val raw  = id.text       // String — matched characters
    val pos  = id.position   // Int — character position
    val ln   = id.line       // Int — line number
    // Use for error reporting:
    ctx.errors.append(("undefined", id, id.line))
}
```

Field access is typed via the `Selectable` refinement on `Lexeme`.
If the lexer uses `LexerCtx.Default`, all four fields (`value`, `text`, `position`, `line`) are available.
Custom context fields (e.g., `id.indentLevel`) are accessible if the lexer context declares them.

**Pitfall:** `position` records the post-match cursor position (after advancing by the token length), not the start position.
For a token `"42"` starting at column 1, `position` is 3. See [Between Stages](between-stages.html) for the exact semantics.

---

See [Parser](parser.html) for grammar rules, rule definitions, and parsing input.
