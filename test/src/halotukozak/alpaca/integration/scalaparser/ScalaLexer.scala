package halotukozak
package alpaca
package integration.scalaparser

import alpaca.Token

/**
 * Lexer for a subset of Scala expressions.
 *
 * Tokens follow the Scala 3 specification lexical syntax
 * (https://docs.scala-lang.org/scala3/reference/syntax.html).
 * Multi-character tokens are listed before single-character ones. Keywords
 * are listed before the identifier pattern; since matching is longest-match
 * with ties broken by pattern order, an input like `iffy` still lexes as a
 * single `id` token (the identifier pattern matches all 4 characters, which
 * is strictly longer than the 2-character `if` keyword match).
 */
val ScalaLexer = lexer:
  // Whitespace and single-line comments are ignored
  case _ @ "[ \t\r\n]+" => Token.Ignored
  case _ @ "//[^\n]*\n?" => Token.Ignored

  // Keywords – listed before the identifier pattern (see note above)
  case "if" => Token["if"]
  case "else" => Token["else"]
  case "val" => Token["val"]
  case "var" => Token["var"]
  case "def" => Token["def"]
  case "class" => Token["class"]
  case "object" => Token["object"]
  case "trait" => Token["trait"]
  case "match" => Token["match"]
  case "case" => Token["case"]
  case "new" => Token["new"]
  case "extends" => Token["extends"]
  case "with" => Token["with"]
  case "while" => Token["while"]
  case "throw" => Token["throw"]
  case "return" => Token["return"]
  case "import" => Token["import"]
  // Modifiers (Scala 3 spec: LocalModifier / AccessModifier / Modifier)
  case "private" => Token["private"]
  case "protected" => Token["protected"]
  case "final" => Token["final"]
  case "sealed" => Token["sealed"]
  case "abstract" => Token["abstract"]
  case "override" => Token["override"]
  case "lazy" => Token["lazy"]
  case "implicit" => Token["implicit"]

  case "true" => Token["true"]
  case "false" => Token["false"]
  case "null" => Token["null"]

  // Multi-character operators (must come before single-character ones)
  case "==" => Token["eqeq"]
  case "!=" => Token["neq"]
  case "<=" => Token["lte"]
  case ">=" => Token["gte"]
  case "&&" => Token["and"]
  case "\\|\\|" => Token["or"]
  case "=>" => Token["arrow"]

  // Wildcard — named because `_` is reserved in Scala identifiers
  case "_" => Token["wildcard"]

  // Single-character operators and punctuation
  case literal @ ("\\+" | "-" | "\\*" | "/" | "%" | "<" | ">" | "!" | "=" | "\\." | "," | ";" | ":" | "@" | "\\|" |
      "\\(" | "\\)" | "\\{" | "\\}" | "\\[" | "\\]") =>
    Token[literal.type]

  // Floating-point literals (before integers to ensure correct matching)
  case x @ """(\d+\.\d+|\.\d+)([eE][+-]?\d+)?""" => Token["floatLit"](x.toDouble)

  // Integer literals
  case x @ """\d+""" => Token["intLit"](x.toLong)

  // Character literals — simplified: store the raw middle content as String.
  // Single unescaped char: `'a'` => "a". Escape sequences like `'\n'` stored
  // as the 2-char String "\\n" — the caller post-processes if it cares.
  case x @ """'(\\.|[^'])'""" => Token["charLit"](x.slice(1, x.length - 1))

  // Interpolated string literals: s"..." / f"..." / raw"..."
  // The prefix identifier and the raw contents are kept verbatim; `$var` /
  // `${expr}` splices are NOT re-lexed — consumer decides.
  case x @ """(s|f|raw)"(\\.|[^"])*"""" => Token["interpStr"](x)

  // String literals (basic, supports backslash-escape sequences)
  case x @ """"(\\.|[^"])*"""" => Token["stringLit"](x.slice(1, x.length - 1))

  // Identifiers (must come after keywords)
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
