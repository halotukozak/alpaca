# Multi-Pass Processing

Alpaca has no dedicated multi-pass API; multi-pass is a composition pattern -- tokenize the input with a first lexer, transform the resulting `List[Lexeme]` in plain Scala, then parse or re-lex as needed.

> **Compile-time processing:** Both the lexer and parser macros are compiled independently; the `List[Lexeme]` boundary between them is an ordinary runtime value you can inspect and transform with any Scala collection operations.

## The Pattern

`tokenize()` returns a named tuple `(ctx, lexemes: List[Lexeme])`; `lexemes` is an ordinary `List` you can `filter`, `map`, or chain to a second stage.
`parse()` accepts any `List[Lexeme]` directly -- the type refinement is widened at the call site, so filtered or re-ordered lists are compatible without any casting.
Each `Lexeme` has a `name: String` field (the token name) and a `value: Any` field (the extracted value) that you can inspect during transformation.

Important constraint: the `Lexeme` constructor is private to the `alpaca` package. You cannot create new `Lexeme` instances. Multi-pass works by transforming the list of existing lexemes -- filter, reorder, or re-lex string values using a second lexer call.

## Example: Comment Stripping

The most common multi-pass pattern: lex input that contains comments, strip the comment tokens from the list, then parse the clean token stream.

```scala sc:nocompile
import alpaca.*

// Stage 1: lex with comments
val Stage1 = lexer:
  case "#.*" => Token["COMMENT"]
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

object SumParser extends Parser:
  val Sum: Rule[Int] = rule(
    { case (Sum(a), Stage1.PLUS(_), Sum(b)) => a + b },
    { case Stage1.NUM(n) => n.value.asInstanceOf[Int] },
  )
  val root: Rule[Int] = rule:
    case Sum(s) => s

// Multi-pass: lex, filter, parse
val (_, stage1Lexemes) = Stage1.tokenize("1 + # ignore this\n2")
val filtered = stage1Lexemes.filter(_.name != "COMMENT")
val (_, result) = SumParser.parse(filtered)
// result: Int | Null  -- 3
```

## Example: Re-Lexing Values

For more advanced cases, string values extracted from stage 1 tokens can be tokenized again by a second lexer and then `flatMap`-ed back into the stream:

```scala sc:nocompile
import alpaca.*

// Advanced: re-lex string values extracted from stage 1 tokens
val (_, tokens) = IdentLexer.tokenize(source)
val expanded = tokens.flatMap:
  case lex if lex.name == "MACRO" =>
    MacroLexer.tokenize(expandMacro(lex.value.asInstanceOf[String])).lexemes
  case lex => List(lex)
val (_, result) = MainParser.parse(expanded)
```

`lex.value` is `Any`; cast to the expected type. `expandMacro` is application-defined.

## Key Points

- `tokenize()` returns `(ctx, lexemes)`; use `.lexemes` or destructure to get the `List[Lexeme]`
- `Lexeme.name` is a `String` (the token name), `Lexeme.value` is `Any` (the extracted value)
- The `Lexeme` constructor is private -- you cannot construct new `Lexeme` instances; work with the existing list
- `parse()` accepts any `List[Lexeme[?, ?]]`; the type refinement is widened at the call site
- `Token.Ignored` tokens produce no lexemes and are never in the list

## See Also

- [Between Stages](../between-stages.html) -- Lexeme structure, `tokenize()` return type, `BetweenStages` hook
- [Lexer](../lexer.html) -- `tokenize()` API, `Token["NAME"](value)` constructor
- [Parser](../parser.html) -- `parse()` API, `Rule[T]` types
