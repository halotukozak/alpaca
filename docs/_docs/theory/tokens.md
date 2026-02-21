# Tokens and Lexemes

A lexer transforms raw source text into a sequence of structured tokens — the first stage of
compilation. Before writing a lexer, it helps to understand the formal vocabulary: what a token
class is, how individual matches relate to it, and what a lexeme carries.

## Terminal Symbols

In formal grammar, a *terminal symbol* is an atomic element that cannot be broken down further.
It is the end of the line for derivation — terminals represent the actual characters or strings
that appear in source text. In a lexer, each token class acts as a terminal: it names a category
of strings, and no lexer-level expansion applies below it.

See [Context-Free Grammars](cfg.md) for how terminals fit into production rules.

## Token Classes vs Token Instances

It is useful to distinguish three levels:

**Token class** — defines a category of strings by a regular expression. For example, the
`NUMBER` class matches any string of the form `[0-9]+(\.[0-9]+)?`: the integers `"3"`, `"42"`,
and the decimals `"3.14"`, `"0.5"`.

**Token instance** — a specific string found in the input that belongs to a token class. When
the lexer scans `"3 + 4"`, it finds three token instances: the string `"3"` (a NUMBER), the
string `"+"` (a PLUS), and the string `"4"` (another NUMBER).

**Lexeme** — the full record of a token instance: the token class, the matched text, and its
position in the source. Parsing `"3 + 4"` produces three lexemes:

- `NUMBER("3", pos=0)`
- `PLUS("+", pos=2)`
- `NUMBER("4", pos=4)`

The word *lexeme* is used throughout this documentation to mean this complete record.

> **Definition — Lexeme:**
> A *lexeme* is a triple (T, w, pos) where T is a token class, w ∈ L(T) is the matched string
> (a member of the language defined by T's regex), and pos is the position of the end of the
> match in the source text.
> In Alpaca: `Lexeme[Name, Value]` where `Name` is the token class name (a string literal type)
> and `Value` is the Scala type of the extracted value.

## Alpaca's Lexeme Type

In Alpaca, each matched token is represented as a `Lexeme[Name, Value]`. A lexeme carries four
pieces of information:

- `name` — the token class name string, e.g., `"NUMBER"` or `"PLUS"`
- `value` — the extracted value with its Scala type, e.g., `3.14: Double` for NUMBER, `(): Unit`
  for PLUS
- `position` — the character offset at the end of the match
- `line` — the line number at the end of the match

The tokenization output for a simple expression illustrates this:

```scala sc:nocompile
val (_, lexemes) = CalcLexer.tokenize("3 + 4 * 2")
// lexemes: List[Lexeme] =
//   NUMBER(3.0), PLUS, NUMBER(4.0), TIMES, NUMBER(2.0)
//
// Each Lexeme carries:
//   .name     — token class name (e.g., "NUMBER")
//   .value    — extracted value  (e.g., 3.0: Double)
//   .position — character offset at end of match
//   .line     — line number at end of match
```

Whitespace matches `Token.Ignored` and does not produce a lexeme — it disappears from the stream.

## CalcLexer Token Class Table

The `CalcLexer` running example defines seven token classes:

| Token Class | Regex Pattern | Value Type | Example Match |
|-------------|--------------|------------|---------------|
| `NUMBER` | `[0-9]+(\.[0-9]+)?` | `Double` | `"3.14"` → `3.14` |
| `PLUS` | `\+` | `Unit` | `"+"` |
| `MINUS` | `-` | `Unit` | `"-"` |
| `TIMES` | `\*` | `Unit` | `"*"` |
| `DIVIDE` | `/` | `Unit` | `"/"` |
| `LPAREN` | `\(` | `Unit` | `"("` |
| `RPAREN` | `\)` | `Unit` | `")"` |

Whitespace is ignored (`Token.Ignored`) and does not appear in the lexeme stream.

`NUMBER` is the only value-bearing token: the macro uses the `@` binding to convert the matched
string to a `Double`. The remaining six tokens carry `Unit` — their presence in the stream is
enough; no value needs to be extracted.

## Full CalcLexer Definition

The canonical CalcLexer definition, which appears throughout the documentation as a running
example:

```scala sc:nocompile
import alpaca.*

val CalcLexer = lexer:
  case num @ "[0-9]+(\\.[0-9]+)?" => Token["NUMBER"](num.toDouble)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["TIMES"]
  case "/" => Token["DIVIDE"]
  case "\\(" => Token["LPAREN"]
  case "\\)" => Token["RPAREN"]
  case "\\s+" => Token.Ignored
```

Each `case` arm maps a Java regex pattern to a token constructor. Patterns are tested in order;
the first match wins. The `num @` binding in the first arm captures the matched text as a
`String`, which `num.toDouble` converts to a `Double` before it is stored in the lexeme.

This definition uses `sc:nocompile` because `lexer` is a Scala 3 macro: the macro runs at
compile time, validates all regex patterns, checks for shadowing, and generates the
`Tokenization` object. See [The Lexer: Regex to Finite Automata](lexer-fa.md) for what the
macro does internally.

## Cross-links

- See [Lexer](../lexer.md) for the full `lexer` DSL reference and all token forms.
- See [The Lexer: Regex to Finite Automata](lexer-fa.md) for how regex patterns define token
  classes formally.
