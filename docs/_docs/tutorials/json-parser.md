# Tutorial: Building a JSON Parser

This tutorial builds a JSON parser with Alpaca, covering lexer definition, recursive grammar rules, and nested data
structures.

## 1. Defining the Lexer

```scala
import alpaca.*

val JsonLexer = lexer:
  case "\\s+" => Token.Ignored
  case "\\{" => Token["{"]
  case "\\}" => Token["}"]
  case "\\[" => Token["["]
  case "\\]" => Token["]"]
  case ":" => Token[":"]
  case "," => Token[","]
  case x@("false" | "true") => Token["Bool"](x.toBoolean)
  case "null" => Token["Null"](null)
  case x@"""[-+]?\d+(\.\d+)?""" => Token["Number"](x.toDouble)
  case x@""""(\\.|[^"])*"""" => Token["String"](x.slice(1, x.length - 1))
```

## 2. Defining the Parser

JSON is recursive: a `Value` can be an `Object` or `Array`, which contain more `Value`s.

```scala
object JsonParser extends Parser:
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

  val Object: Rule[Map[String, Any]] = rule(
    { case (JsonLexer.`{`(_), JsonLexer.`}`(_)) => Map.empty[String, Any] },
    { case (JsonLexer.`{`(_), ObjectMembers(members), JsonLexer.`}`(_)) => members.toMap },
  )

  val ObjectMembers: Rule[List[(String, Any)]] = rule(
    { case ObjectMember(member) => scala.List(member) },
    { case (ObjectMembers(members), JsonLexer.`,`(_), ObjectMember(member)) => members :+ member },
  )

  val ObjectMember: Rule[(String, Any)] = rule:
    case (JsonLexer.String(s), JsonLexer.`:`(_), Value(v)) => (s.value, v)

  val Array: Rule[List[Any]] = rule(
    { case (JsonLexer.`[`(_), JsonLexer.`]`(_)) => Nil },
    { case (JsonLexer.`[`(_), ArrayElements(elems), JsonLexer.`]`(_)) => elems },
  )

  val ArrayElements: Rule[List[Any]] = rule(
    { case Value(v) => scala.List(v) },
    { case (ArrayElements(elems), JsonLexer.`,`(_), Value(v)) => elems :+ v },
  )
```

## 3. Parsing Input

```scala
val input = """{"name": "Alice", "age": 30, "tags": ["a", "b"]}"""

val (_, lexemes) = JsonLexer.tokenize(input)
val (_, result) = JsonParser.parse(lexemes)

println(result)
// Map(name -> Alice, age -> 30.0, tags -> List(a, b))
```

## 4. Using EBNF Operators

Alpaca provides `.List` and `.Option` operators on **Rules** to simplify common patterns.

> `.List` and `.Option` work on `Rule` references, not on token references directly.

For example, instead of writing explicit recursion for a list of arguments:

```scala
val FnCall: Rule[List[Any]] = rule:
  case (MyLexer.`(`(_), Arg.List(args), MyLexer.`)`(_)) => args

val Arg: Rule[Any] = rule:
  case Value(v) => v
```

For separator-delimited lists (like JSON's comma-separated members), explicit recursion as shown in Section 2 is
often clearer.
