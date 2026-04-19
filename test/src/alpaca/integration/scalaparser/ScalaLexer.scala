package alpaca
package integration.scalaparser

/**
 * Lexer for a subset of Scala expressions.
 *
 * Tokens follow the Scala 3 specification lexical syntax.
 * Multi-character tokens are listed before single-character ones to ensure
 * they are matched first. Keywords use a word-boundary assertion to prevent
 * matching as prefixes of identifiers (e.g. "if" must not match in "iffy").
 */
val ScalaLexer = lexer:
  // Whitespace and single-line comments are ignored
  case _ @ "[ \t\r\n]+"  => Token.Ignored
  case _ @ "//[^\n]*\n?" => Token.Ignored

  // Keywords – word-boundary assertion prevents matching identifier prefixes
  case "if\\b"    => Token["if"]
  case "else\\b"  => Token["else"]
  case "val\\b"   => Token["val"]
  case "true\\b"  => Token["true"]
  case "false\\b" => Token["false"]
  case "null\\b"  => Token["null"]

  // Multi-character operators (must come before single-character ones)
  case "==" => Token["eqeq"]
  case "!=" => Token["neq"]
  case "<=" => Token["lte"]
  case ">=" => Token["gte"]
  case "&&" => Token["and"]
  case "||" => Token["or"]

  // Single-character operators and punctuation
  case literal @ ("\\+" | "-" | "\\*" | "/" | "%" | "<" | ">" | "!" | "=" |
                  "\\." | "," | ";" | "\\(" | "\\)" | "\\{" | "\\}") =>
    Token[literal.type]

  // Floating-point literals (before integers to ensure correct matching)
  case x @ """(\d+\.\d+|\.\d+)([eE][+-]?\d+)?""" => Token["floatLit"](x.toDouble)

  // Integer literals
  case x @ """\d+""" => Token["intLit"](x.toLong)

  // String literals (basic, supports backslash-escape sequences)
  case x @ """"(\\.|[^"])*"""" => Token["stringLit"](x.slice(1, x.length - 1))

  // Identifiers (must come after keywords)
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
