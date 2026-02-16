# Tutorial: Building a JSON Parser

In this tutorial, we will build a fully functional JSON parser using Alpaca. This will demonstrate how to define a lexer, a parser, and how to handle nested data structures.

## 1. Defining the Lexer

First, we need to define the lexical tokens for JSON, including braces, brackets, colons, commas, and literals like strings, numbers, booleans, and null.

```scala
import alpaca.*
import annotation.nowarn

val JsonLexer = lexer:
  // Ignore whitespace
  case "\s+" => Token.Ignored

  // Brackets and punctuation
  case "\{" => Token["{"]
  case "\}" => Token["}"]
  case "\[" => Token["["]
  case "\]" => Token["]"]
  case ":" => Token[":"]
  case "," => Token[","]

  // Literals
  case x @ ("false" | "true") => Token["Bool"](x.toBoolean)
  case "null" => Token["Null"](null)

  // Numbers and strings
  case x @ """[-+]?\d+(\.\d+)?""" => Token["Number"](x.toDouble)
  case x @ """"(\.|[^"])*"""" => Token["String"](x.slice(1, x.length - 1))
```

## 2. Defining the Parser

Now we define the grammar rules for JSON. JSON has a recursive structure where a `Value` can be an `Object` or an `Array`, which in turn contain more `Value`s.

```scala
object JsonParser extends Parser:
  // The root of our grammar
  val root: Rule[Any] = rule:
    case Value(value) => value

  // A JSON value can be any of the following
  val Value: Rule[Any] = rule(
    { case JsonLexer.Null(n) => n.value },
    { case JsonLexer.Bool(b) => b.value },
    { case JsonLexer.Number(n) => n.value },
    { case JsonLexer.String(s) => s.value },
    { case Object(obj) => obj },
    { case Array(arr) => arr },
  )

  // Objects are collections of key-value pairs wrapped in braces
  val Object: Rule[Map[String, Any]] = rule(
    { case (JsonLexer.`{`(_), JsonLexer.`}`(_)) => Map.empty[String, Any] },
    { case (JsonLexer.`{`(_), ObjectMembers(members), JsonLexer.`}`(_)) => members.toMap },
  )

  // Helper rule for a list of object members
  val ObjectMembers: Rule[List[(String, Any)]] = rule(
    { case ObjectMember(member) => List(member) },
    { case (ObjectMembers(members), JsonLexer.`,`(_), ObjectMember(member)) => members :+ member },
  )

  // A single key-value pair
  val ObjectMember: Rule[(String, Any)] = rule:
    case (JsonLexer.String(s), JsonLexer.`:`(_), Value(v)) => (s.value, v)

  // Arrays are ordered lists of values wrapped in brackets
  val Array: Rule[List[Any]] = rule(
    { case (JsonLexer.`[`(_), JsonLexer.`]`(_)) => Nil },
    { case (JsonLexer.`[`(_), ArrayElements(elems), JsonLexer.`]`(_)) => elems },
  )

  // Helper rule for a list of array elements
  val ArrayElements: Rule[List[Any]] = rule(
    { case Value(v) => List(v) },
    { case (ArrayElements(elems), JsonLexer.`,`(_), Value(v)) => elems :+ v },
  )
```

## 3. Parsing Input

With our lexer and parser defined, we can now parse JSON strings:

```scala
val input = """
{
  "name": "John Doe",
  "age": 30,
  "isStudent": false,
  "courses": ["Math", "Science"],
  "address": {
    "city": "Anytown",
    "zip": "12345"
  },
  "nullValue": null
}
"""

val (_, lexemes) = JsonLexer.tokenize(input)
val (_, result) = JsonParser.parse(lexemes)

println(result)
// Output: Map(name -> John Doe, age -> 30.0, isStudent -> false, ...)
```

## 4. Refactoring with EBNF Operators

Alpaca's built-in `List` and `Option` operators can simplify our grammar significantly by removing the need for explicit helper rules like `ObjectMembers` and `ArrayElements`.

```scala
object ModernJsonParser extends Parser:
  val root: Rule[Any] = rule:
    case Value(value) => value

  val Value: Rule[Any] = rule(
    { case JsonLexer.Null(n) => n.value },
    { case JsonLexer.Bool(b) => b.value },
    { case JsonLexer.Number(n) => n.value },
    { case JsonLexer.String(s) => s.value },
    { case Object(obj) => obj },
    { case Array(arr) => arr },
  )

  val Object: Rule[Map[String, Any]] = rule:
    case (JsonLexer.`{`(_), ObjectMembers.List(m), JsonLexer.`}`(_)) => m.toMap

  val ObjectMembers: Rule[(String, Any)] = rule:
    case (ObjectMember(m), JsonLexer.`,`(_).Option) => m

  val ObjectMember: Rule[(String, Any)] = rule:
    case (JsonLexer.String(s), JsonLexer.`:`(_), Value(v)) => (s.value, v)

  val Array: Rule[List[Any]] = rule:
    case (JsonLexer.`[`(_), ArrayElements.List(e), JsonLexer.`]`(_)) => e

  val ArrayElements: Rule[Any] = rule:
    case (Value(v), JsonLexer.`,`(_).Option) => v
```

> **Note:** The above refactoring uses `.List` and `.Option`. Note that in a real JSON grammar, commas are separators and cannot follow the last element. The simple `.Option` approach here might be slightly more permissive than the strict JSON spec, but it demonstrates the power of the EBNF operators.

## Summary

In this tutorial, we:
1. Defined a `lexer` with regex patterns for JSON tokens.
2. Defined a `Parser` with recursive rules for objects and arrays.
3. Used `rule` blocks and pattern matching to transform tokens into Scala data structures.
4. Explored how `List` and `Option` can simplify grammar definitions.
