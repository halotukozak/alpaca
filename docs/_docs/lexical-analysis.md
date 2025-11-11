Now I'll create a comprehensive tutorial on lexical analysis basics and ALPACA parser generator with examples.

# Lexical Analysis and ALPACA Parser Generator: A Complete Tutorial

## Part 1: Understanding Lexical Analysis

### What is a Lexer?

A **lexer** (also called a **lexical analyzer**, **scanner**, or **tokenizer**) is the first phase of a compiler that reads source code as a sequence of characters and converts it into a sequence of **tokens**. Think of it as breaking down sentences into individual words with their meanings.

**Key responsibilities of a lexer:**
- Extract lexical symbols (tokens) from source code
- Discard comments and whitespace
- Correlate error messages with line numbers
- Create a symbol table for identifiers

### Core Concepts

#### Token
A **token** is a (name, attribute) pair representing a syntactic category. The token name is passed to the parser, while attributes provide additional information.

**Example tokens:**
- `KEYWORD` - reserved words like `if`, `while`, `for`
- `ID` - identifiers (variable names)
- `NUM` - numeric constants
- `RELOP` - relational operators (`<`, `>`, `==`)
- Literals - single characters like `(`, `)`, `;`

#### Lexeme
A **lexeme** is the actual sequence of characters in the source code that matches a token's pattern.

#### Pattern
A **pattern** describes the set of lexemes that can represent a particular token, typically specified using regular expressions.

### Example: Token, Lexeme, and Pattern

Consider the code: `const pi = 3.1416`

| **Token** | **Lexeme** | **Pattern** |
|-----------|-----------|-------------|
| CONST | `const` | `const` |
| ID | `pi` | `[a-zA-Z][a-zA-Z0-9]*` |
| ASSIGN | `=` | `=` |
| NUM | `3.1416` | `[0-9]+(\.[0-9]+)?` |

### How a Lexer Works

**Input:** Source code as a character stream
```
cost = price + tax * 0.98
```

**Output:** Token stream
```
ID(cost) ASSIGN ID(price) PLUS ID(tax) MUL NUM(0.98)
```

**Symbol Table:**

| Address | Name | Type | Value |
|---------|------|------|-------|
| 1 | cost | variable | - |
| 2 | price | variable | - |
| 3 | tax | variable | - |
| 4 | 0.98 | constant | 0.98 |

## Part 2: Defining a Lexer with ALPACA

ALPACA is a Scala-based lexer framework that provides an elegant DSL (Domain-Specific Language) for defining lexers using pattern matching and regular expressions. This tutorial will guide you through creating lexers with ALPACA.


## Basic Lexer Structure

The fundamental syntax for defining a lexer in ALPACA is:

```scala
import alpaca.api.*

val myLexer = lexer:
  case pattern @ "regex" => Token["TOKEN_NAME"](value)
```

**Key components:**
- `lexer { ... }` - Creates a new lexer instance
- `case` - Pattern matching for each token rule
- `pattern @` - Captures the matched text (optional)
- `"regex"` - Regular expression as a string literal
- `Token["NAME"]` - Defines the token type
- `(value)` - Optional semantic value

## Example 1: Simple Identifier Lexer

The most basic lexer recognizes a single token type:

```scala
import alpaca.api.*

val identifierLexer = lexer:
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)

val result = identifierLexer.tokenize("hello")
// Result: List(Lexem("IDENTIFIER", "hello"))
```

**Explanation:**
- Pattern `[a-zA-Z][a-zA-Z0-9]*` matches identifiers starting with a letter
- `id @` captures the matched text
- `Token["IDENTIFIER"](id)` creates a token with the matched text as its value

## Example 2: Ignoring Whitespace

Real-world lexers need to skip whitespace and other irrelevant characters:

```scala
import alpaca.api.*

val calculatorLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored

val result = calculatorLexer.tokenize("42 + 13")
// Result: List(
//   Lexem("NUMBER", "42"),
//   Lexem("PLUS", ()),
//   Lexem("NUMBER", "13")
// )
```

**Key points:**
- `Token.Ignored` - Matches and discards tokens (like whitespace)
- `\\+` - Escapes special regex characters
- `Token["PLUS"]` without value creates a token with `Unit` value

## Example 3: Complex Expression Lexer

A practical lexer for arithmetic expressions handles multiple token types:

```scala
import alpaca.api.*

val expressionLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number)
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["IDENTIFIER"](id)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["MULTIPLY"]
  case "\\(" => Token["LPAREN"]
  case "\\)" => Token["RPAREN"]
  case "\\s+" => Token.Ignored

val result = expressionLexer.tokenize("(x + 42) * y - 1")
// Result: List(
//   Lexem("LPAREN", ()),
//   Lexem("IDENTIFIER", "x"),
//   Lexem("PLUS", ()),
//   Lexem("NUMBER", "42"),
//   Lexem("RPAREN", ()),
//   Lexem("MULTIPLY", ()),
//   Lexem("IDENTIFIER", "y"),
//   Lexem("MINUS", ()),
//   Lexem("NUMBER", "1")
// )
```

## Example 4: Type Conversion with Semantic Values

ALPACA allows you to transform matched text into typed values:

```scala
import alpaca.api.*

val typedLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)

val result = typedLexer.tokenize("123")
// The token value is Int, not String
```

**Important:** When parsing fails, ALPACA throws a `RuntimeException`.

## Example 5: Programming Language Lexer

A comprehensive lexer for a full programming language:

```scala
import alpaca.api.*

val programmingLangLexer = lexer:
  // Comments (ignored)
  case comment @ "#.*" => Token.Ignored
  
  // Operators (using alternation)
  case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | 
                  "\\(" | "\\)" | "\\[" | "\\]" | "{" | "}" | 
                  ":" | "'" | "," | ";") => 
    Token[literal.type]
  
  // Compound operators
  case "\\.\\+" => Token["dotAdd"]
  case "\\.\\-" => Token["dotSub"]
  case "\\.\\*" => Token["dotMul"]
  case "\\./" => Token["dotDiv"]
  case "<=" => Token["lessEqual"]
  case ">=" => Token["greaterEqual"]
  case "!=" => Token["notEqual"]
  case "==" => Token["equal"]
  
  // Numeric literals
  case x @ "(d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" => Token["float"](x.toDouble)
  case x @ "[0-9]+" => Token["int"](x.toInt)
  
  // String literals
  case x @ "\"[^\"]*\"" => Token["string"](x)
  
  // Keywords (using alternation)
  case keyword @ ("if" | "else" | "for" | "while" | "break" | 
                  "continue" | "return" | "eye" | "zeros" | 
                  "ones" | "print") =>
    Token[keyword.type]
  
  // Identifiers (must come after keywords!)
  case x @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["id"](x)
```

## Key Features and Best Practices

### 1. Token Alternation

Use the `|` operator to match multiple patterns with the same token type:

```scala sc:nocompile
case literal @ ("<" | ">" | "=") => Token[literal.type]
```

The `literal.type` preserves the exact matched string as the token name.

### 2. Pattern Order Matters

**Critical:** More specific patterns must come before general ones:

```scala
import alpaca.api.*

// CORRECT ORDER
val validLexer = lexer:
    case "if" => Token["if"]           // Keyword first
    case "else" => Token["else"]       // Keyword first
    case x @ "[a-zA-Z]+" => Token["id"](x)  // General identifier last

// WRONG - keywords would be matched as identifiers!
val invalidLexer = lexer:
    case x @ "[a-zA-Z]+" => Token["id"](x)
    case "if" => Token["if"]
```

### 3. Overlapping Pattern Detection

ALPACA detects overlapping patterns at compile time:[

```scala sc:fail
import alpaca.api.*

// This will NOT compile - patterns overlap!
val invalidLexer = lexer:
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "[a-zA-Z]+" => Token["ALPHABETIC"]  // Error: overlaps with above
```

### 4. Accessing Lexer Fields

ALPACA generates type-safe accessors for all defined tokens:

```scala
import alpaca.api.*

val myLexer = lexer:
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\+" => Token["PLUS"]

// Type-safe access to tokens
myLexer.NUMBER  // Token["NUMBER", DefaultGlobalCtx, Int]
myLexer.PLUS    // Token["PLUS", DefaultGlobalCtx, Unit]

// Get all tokens as a list
myLexer.tokens  // List[Token[?, DefaultGlobalCtx, ?]]
```

### 5. Tokenizing from Files

ALPACA supports tokenizing from strings and files:

```scala sc:nocompile
import alpaca.api.*
import java.nio.file.Path
// From string
val result1 = myLexer.tokenize("42 + 13")

// From file
val reader = LazyReader.from(Path.of("input.txt"))
val result2 = myLexer.tokenize(reader)
```

## Advanced Features

### Context-Aware Tokenization

ALPACA supports stateful lexing with custom contexts, which can be accessed and modified with `ctx`:

```scala
import alpaca.api.*
import alpaca.lexer.context.*

case class MyContext(var digitcounter: Int) extends GlobalCtx

val contextAwareLexer = lexer[MyContext]:
  case "\\d" => 
    ctx.digitcounter += 1
    Token["digit"]
```

### Semantic Actions with Context Manipulation

You can execute code when a token is matched:

```scala
import alpaca.api.*

val statefulLexer = lexer:
  case number @ "[0-9]+" => 
    // Execute statements before creating token
    val parsed = number.toInt
    if (parsed > 100) println("Large number!")
    Token["NUMBER"](parsed)
```

## Complete Working Example

Here's a complete lexer for a simple calculator language:

```scala
import alpaca.api.*
import alpaca.lexer.context.*

val calculatorLexer = lexer:
  // Skip whitespace
  case "\\s+" => Token.Ignored
  
  // Numbers
  case num @ "[0-9]+\\.[0-9]+" => Token["FLOAT"](num.toDouble)
  case num @ "[0-9]+" => Token["INT"](num.toInt)
  
  // Operators
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["MULTIPLY"]
  case "/" => Token["DIVIDE"]
  case "\\^" => Token["POWER"]
  
  // Parentheses
  case "\\(" => Token["LPAREN"]
  case "\\)" => Token["RPAREN"]
  
  // Functions
  case fn @ ("sin" | "cos" | "tan" | "log" | "sqrt") => Token["FUNCTION"](fn)
  
  // Variables
  case id @ "[a-zA-Z][a-zA-Z0-9]*" => Token["VAR"](id)

// Use the lexer
val input = "2.5 + sin(x) * 3"
val tokens = calculatorLexer.tokenize(input)

println(tokens)
```

**Output:**
```
Lexem(FLOAT,2.5)
Lexem(PLUS,())
Lexem(FUNCTION,sin)
Lexem(LPAREN,())
Lexem(VAR,x)
Lexem(RPAREN,())
Lexem(MULTIPLY,())
Lexem(INT,3)
```

## Advanced Implementation: Anonymous Classes and Type Refinement

This section explores the internal implementation of ALPACA's lexer macro, focusing on how anonymous classes and type refinement enable type-safe token access.

### How ALPACA Creates Lexers at Compile Time

When you define a lexer using ALPACA's DSL, the `lexer` macro performs sophisticated compile-time code generation. Understanding this process reveals why ALPACA provides such excellent type safety and IDE support.

### Anonymous Class Generation

At the heart of ALPACA's implementation is the creation of an **anonymous class** at compile time. Here's how it works:

```scala
val myLexer = lexer {
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\+" => Token["PLUS"]
}
```

The macro transforms this DSL into something conceptually similar to:

```scala
val myLexer = new Tokenization[DefaultGlobalCtx] {
  val NUMBER: DefinedToken["NUMBER", DefaultGlobalCtx, Int] = ...
  val PLUS: DefinedToken["PLUS", DefaultGlobalCtx, Unit] = ...
  
  protected def compiled: Regex = "(?<token0>[0-9]+)|(?<token1>\\+)".r
  def tokens: List[Token[?, DefaultGlobalCtx, ?]] = List(NUMBER, PLUS)
  def byName: Map[String, DefinedToken[?, DefaultGlobalCtx, ?]] = Map("NUMBER" -> NUMBER, "PLUS" -> PLUS)
}
```

**Implementation details** (from `Lexer.scala`, lines 201-207):

```scala
val cls = Symbol.newClass(
  Symbol.spliceOwner,
  Symbol.freshName("$anon"),      // Creates a unique anonymous class name
  List(TypeRepr.of[Tokenization[Ctx]]),  // Extends Tokenization
  decls,                          // Field and method declarations
  None,
)
```

### Type Refinement: Adding Token Fields to the Type

The most powerful feature is **type refinement**, which adds each token as a field directly to the lexer's type. This enables:

1. **Type-safe token access**: `myLexer.NUMBER` is known at compile time
2. **IDE autocompletion**: Your editor suggests available tokens
3. **Compile-time validation**: Typos in token names cause compilation errors

**How refinement works** (from `Lexer.scala`, lines 267-276):

```scala
definedTokens
  .foldLeft(TypeRepr.of[Tokenization[Ctx]]):
    case (tpe, token) =>
      Refinement(tpe, tokenName, tokenType)  // Adds field to type
  .asType match
    case '[refinedTpe] =>
      // Returns: Tokenization[Ctx] & { val NUMBER: ...; val PLUS: ... }
      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
```

The result is an **intersection type** (structural type) that includes both:
- The base `Tokenization[Ctx]` functionality
- Individual fields for each defined token

### Type Refinement in Action

Consider this lexer:

```scala
val calc = lexer {
  case num @ "[0-9]+" => Token["NUM"](num.toInt)
  case "\\+" => Token["ADD"]
}
```

The inferred type becomes:

```scala
Tokenization[DefaultGlobalCtx] {
  val NUM: DefinedToken["NUM", DefaultGlobalCtx, Int]
  val ADD: DefinedToken["ADD", DefaultGlobalCtx, Unit]
}
```

This allows you to:

```scala
// Access tokens with full type information
calc.NUM  // Type: DefinedToken["NUM", DefaultGlobalCtx, Int]
calc.ADD  // Type: DefinedToken["ADD", DefaultGlobalCtx, Unit]

// Use in pattern matching with type safety
def parseExpression(tokens: List[Token[?, DefaultGlobalCtx, ?]]): Unit = tokens match
  case calc.NUM :: calc.ADD :: calc.NUM :: Nil => println("Valid expression")
  case _ => println("Invalid")
```

### Why This Approach?

ALPACA uses anonymous classes and type refinement for several compelling reasons:

#### 1. **Zero Runtime Overhead**

All token definitions are resolved at compile time. There's no reflection, no runtime type checking, and no dictionary lookups for token access.

```scala
myLexer.NUMBER  // Direct field access, not a map lookup
```

#### 2. **Maximum Type Safety**

Each token has a precise type including its name and value type:

```scala
val lexer = lexer {
  case x @ "[0-9]+" => Token["INT"](x.toInt)
  case x @ "[0-9]+\\.[0-9]+" => Token["FLOAT"](x.toDouble)
}

lexer.INT    // Type: DefinedToken["INT", DefaultGlobalCtx, Int]
lexer.FLOAT  // Type: DefinedToken["FLOAT", DefaultGlobalCtx, Double]
```

The compiler knows the exact value type of each token, preventing type errors.

#### 3. **Excellent IDE Support**

Because tokens are actual fields in the type, IDEs can:
- Provide autocompletion for token names
- Show token types on hover
- Offer go-to-definition navigation
- Detect typos before compilation

#### 4. **Compile-Time Pattern Overlap Detection**

The macro analyzes all patterns at compile time:

```scala
// This will NOT compile - patterns overlap!
val invalid = lexer {
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "[a-zA-Z]+" => Token["ALPHABETIC"]  // Error: overlaps with above
}
```

ALPACA detects this conflict during compilation, not at runtime.

#### 5. **Structural Typing with Nominal Benefits**

The refined type is structural (you access fields by name), but each field has a precise nominal type. This combines the flexibility of structural typing with the safety of nominal typing.

### Alternative Approaches and Trade-offs

Why not use simpler approaches?

#### Alternative 1: Map-Based Token Storage

```scala
class SimpleLexer {
  val tokens: Map[String, Token[?, ?, ?]] = Map(
    "NUMBER" -> ...,
    "PLUS" -> ...
  )
  def apply(name: String) = tokens(name)
}

myLexer("NUMBER")  // Runtime lookup, loses type information
```

**Downsides:**
- No type safety: `myLexer("NUMBR")` fails at runtime
- No IDE support
- Runtime overhead of map lookups
- Lost type information: result is `Token[?, ?, ?]`

#### Alternative 2: Explicit Class Definition

```scala
class MyLexer extends Tokenization[DefaultGlobalCtx] {
  val NUMBER = DefinedToken[...]
  val PLUS = DefinedToken[...]
}

val myLexer = new MyLexer
```

**Downsides:**
- Verbose and repetitive
- Requires manual regex compilation
- No DSL for pattern matching
- Error-prone (easy to forget patterns in compiled regex)

### Benefits Summary

ALPACA's anonymous class + type refinement approach provides:

| Feature | Benefit |
|---------|---------|
| **Compile-time generation** | Zero runtime overhead, early error detection |
| **Type refinement** | Type-safe token access with precise types |
| **Structural typing** | Flexible field access with compiler validation |
| **Anonymous classes** | Clean DSL without boilerplate |
| **Macro analysis** | Pattern overlap detection, regex validation |
| **IDE integration** | Autocompletion, navigation, inline errors |

This sophisticated approach makes ALPACA both developer-friendly and highly efficient, catching errors early while maintaining excellent runtime performance.

## Error Handling

todo: Add error handling section here.

## Summary

**Essential ALPACA lexer patterns:**

1. **Basic token**: `case "regex" => Token["NAME"]`
2. **With value**: `case x @ "regex" => Token["NAME"](x)`
3. **With conversion**: `case x @ "[0-9]+" => Token["NUM"](x.toInt)`
4. **Ignored token**: `case "\\s+" => Token.Ignored`
5. **Alternation**: `case x @ ("a" | "b") => Token[x.type]`

**Best practices:**
- Put specific patterns (keywords) before general ones (identifiers)
- Always handle whitespace with `Token.Ignored`
- Escape special regex characters: `\\+`, `\\*`, `\$$`, etc.
- Use `@` binding to capture matched text
- Let ALPACA detect pattern overlaps at compile time

ALPACA's type-safe DSL makes lexer development both safe and expressive, catching errors at compile time rather than runtime.[3][2][1]
