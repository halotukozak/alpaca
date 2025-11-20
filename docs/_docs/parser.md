# Syntax Analysis and ALPACA Parser Generator: A Complete Tutorial

## Part 1: Understanding Syntax Analysis

### What is a Parser?

A **parser** (also called a **syntax analyzer**) is the second phase of a compiler that takes a sequence of **tokens** from the lexer and determines whether they form valid sentences according to the grammar of the language. Think of it as checking whether words arranged in a sentence follow proper grammatical rules.

**Key responsibilities of a parser:**
- Verify that tokens appear in patterns allowed by the language grammar
- Build a parse tree or abstract syntax tree (AST) representing the structure
- Detect and report syntax errors
- Pass structured data to the semantic analyzer

### Core Concepts

#### Grammar

A **grammar** is a set of rules that defines the valid structure of sentences in a language. Grammars are typically written in **Backus-Naur Form (BNF)** or **Extended BNF (EBNF)**.

**Example grammar for arithmetic expressions:**
```
Expr → Expr + Term | Expr - Term | Term
Term → Term * Factor | Term / Factor | Factor
Factor → NUMBER | ( Expr )
```

#### Production Rules

A **production rule** defines how a non-terminal symbol can be replaced with a sequence of terminals and non-terminals.

**Example:**
```
Expr → Expr + Term
```
This rule says: "An expression can be an expression followed by '+' followed by a term."

#### Terminals vs Non-terminals

- **Terminals** are tokens from the lexer (e.g., `NUMBER`, `+`, `if`)
- **Non-terminals** are grammar symbols that can be expanded using production rules (e.g., `Expr`, `Statement`)

#### Parse Tree

A **parse tree** (or **concrete syntax tree**) shows the syntactic structure of the input according to the grammar. Each interior node represents a grammar rule application, and leaves represent tokens.

**Example parse tree for `2 + 3 * 4`:**
```
      Expr
     /  |  \
  Expr  +  Term
   |      /  |  \
  Term  Term * Factor
   |     |       |
Factor Factor  NUMBER(4)
   |     |
NUMBER(2) NUMBER(3)
```

### How a Parser Works

**Input:** Token stream from lexer
```
[NUMBER(2), PLUS, NUMBER(3), TIMES, NUMBER(4)]
```

**Process:** Apply grammar rules to build structure
```
1. Recognize NUMBER(2) as Factor → Term → Expr
2. See PLUS, look ahead
3. Recognize NUMBER(3) as Factor → Term
4. See TIMES, which binds tighter than PLUS
5. Recognize NUMBER(4) as Factor
6. Combine into Term (3 * 4)
7. Combine into Expr (2 + (3 * 4))
```

**Output:** Parse result with computed value
```
14
```

### Parser Types

**Top-Down Parsers** (e.g., LL, Recursive Descent)
- Start from the root and work down to leaves
- Use leftmost derivations
- More intuitive but can't handle left recursion

**Bottom-Up Parsers** (e.g., LR, LALR, SLR)
- Start from leaves and work up to root
- Use rightmost derivations in reverse
- More powerful, handle left recursion
- **ALPACA uses LR parsing**

## Part 2: Defining a Parser with ALPACA

ALPACA provides a Scala-based parser framework with an elegant DSL for defining grammars using pattern matching. The parser automatically generates LR parse tables at compile time and provides type-safe parsing with semantic actions.

## Basic Parser Structure

The fundamental syntax for defining a parser in ALPACA:

```scala
import alpaca.api.*

object MyParser extends Parser[EmptyGlobalCtx] {
  val myRule: Rule[ResultType] = rule(
    { case pattern => action }
  )
  
  val root: Rule[ResultType] = rule(
    { case myRule(result) => result }
  )
}
```

**Key components:**
- `Parser[Ctx]` - Base class for parsers, parameterized by context type
- `Rule[R]` - Represents a grammar rule producing results of type `R`
- `rule(...)` - Creates a grammar rule from one or more productions
- `root` - The starting rule of the grammar (must be defined)
- Pattern matching - Define productions using Scala's pattern matching syntax

## Example 1: Simple Expression Parser

The most basic parser recognizes and evaluates simple arithmetic:

```scala
import alpaca.api.*

val CalcLexer = lexer {
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\+" => Token["PLUS"]
  case "\\s+" => Token.Ignored
}

object CalcParser extends Parser[EmptyGlobalCtx] {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
    { case CalcLexer.NUMBER(n) => n.value }
  )
  
  val root = rule { case Expr(result) => result }
}

val tokens = CalcLexer.tokenize("2 + 3 + 4")
val (_, result) = CalcParser.parse[Int](tokens)
// Result: 9
```

**Explanation:**
- `Expr` is a recursive rule with two productions
- First production: `Expr + Expr` combines two expressions
- Second production: `NUMBER` is the base case
- Pattern matching extracts token values with `.value`

## Example 2: Precedence and Associativity

Real parsers need to handle operator precedence correctly:

```scala
import alpaca.api.*

val MathLexer = lexer {
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case "\\+" => Token["PLUS"]
  case "\\*" => Token["TIMES"]
  case "\\s+" => Token.Ignored
}

object MathParser extends Parser[EmptyGlobalCtx] {
  val Factor: Rule[Int] = rule(
    { case MathLexer.NUMBER(n) => n.value }
  )
  
  val Term: Rule[Int] = rule(
    { case (Term(a), MathLexer.TIMES(_), Factor(b)) => a * b },
    { case Factor(f) => f }
  )
  
  val Expr: Rule[Int] = rule(
    { case (Expr(a), MathLexer.PLUS(_), Term(b)) => a + b },
    { case Term(t) => t }
  )
  
  val root = rule { case Expr(result) => result }
}

val tokens = MathLexer.tokenize("2 + 3 * 4")
val (_, result) = MathParser.parse[Int](tokens)
// Result: 14 (not 20, because * has higher precedence)
```

**Key points:**
- Three-level hierarchy: `Expr` → `Term` → `Factor`
- `Factor` handles base values
- `Term` handles multiplication (higher precedence)
- `Expr` handles addition (lower precedence)
- This grammar structure naturally encodes precedence

## Example 3: Conflict Resolution

When using left-recursive grammars (like `Expr → Expr + Expr`), ALPACA may detect shift-reduce conflicts. You can resolve these explicitly:

```scala
import alpaca.api.*
import alpaca.parser.Production as P

object ExprParser extends Parser[EmptyGlobalCtx] {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b }: @name("plus"),
    { case (Expr(a), CalcLexer.TIMES(_), Expr(b)) => a * b }: @name("times"),
    { case CalcLexer.NUMBER(n) => n.value }
  )
  
  val root = rule { case Expr(result) => result }
  
  override val resolutions = Set(
    // Times has higher precedence than plus
    P.ofName("times").before(CalcLexer.PLUS),
    P.ofName("times").before(CalcLexer.TIMES),
    
    // Plus comes after times
    P.ofName("plus").after(CalcLexer.TIMES),
    P.ofName("plus").before(CalcLexer.PLUS)
  )
}
```

**Explanation:**
- `@name("...")` annotation labels productions
- `resolutions` set defines precedence and associativity
- `.before(tokens...)` means reduce production before shifting those tokens
- `.after(tokens...)` means shift those tokens before reducing production
- This gives fine-grained control over ambiguity resolution

## Example 4: Extracting Token Values

ALPACA makes it easy to work with semantic values from tokens:

```scala
import alpaca.api.*

val TypedLexer = lexer {
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case id @ "[a-zA-Z]+" => Token["ID"](id)
  case "\\s+" => Token.Ignored
}

object TypedParser extends Parser[EmptyGlobalCtx] {
  val Value: Rule[Any] = rule(
    { case TypedLexer.NUMBER(n) => n.value },
    { case TypedLexer.ID(id) => id.value }
  )
  
  val root = rule { case Value(v) => v }
}
```

**Important:** Token extractors like `NUMBER(n)` bind the lexem, and `.value` accesses its semantic value.

## Example 5: Complex Grammar with Variables

A parser for assignment statements and expressions:

```scala
import alpaca.api.*
import scala.collection.mutable

case class CalcContext(
  names: mutable.Map[String, Int] = mutable.Map.empty
) extends GlobalCtx

object VarParser extends Parser[CalcContext] {
  val Expr: Rule[Int] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b },
    { case (Expr(a), CalcLexer.MINUS(_), Expr(b)) => a - b },
    { case CalcLexer.NUMBER(n) => n.value },
    { case CalcLexer.ID(id) => 
      ctx.names.getOrElse(id.value, 0)
    }
  )
  
  val Statement: Rule[Unit] = rule(
    { case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(value)) =>
      ctx.names(id.value) = value
    },
    { case Expr(e) => () }
  )
  
  val root = rule { case Statement(s) => s }
}

val tokens = CalcLexer.tokenize("x = 10")
val (ctx, _) = VarParser.parse[Unit](tokens)
// ctx.names contains Map("x" -> 10)
```

## Key Features and Best Practices

### 1. Rule Patterns

Match tokens and other rules in patterns:

```scala
// Match a single token
{ case MyLexer.PLUS(_) => ... }

// Match token with value
{ case MyLexer.NUMBER(n) => n.value }

// Match other rules
{ case (Expr(a), MyLexer.PLUS(_), Expr(b)) => a + b }

// Match nested patterns
{ case (MyLexer.LPAREN(_), Expr(e), MyLexer.RPAREN(_)) => e }
```

### 2. EBNF Support: Option and List

ALPACA supports EBNF-style optional and repeated elements:

```scala
import alpaca.api.*

object EbnfParser extends Parser[EmptyGlobalCtx] {
  val ArgList: Rule[List[Int]] = rule(
    { case (Expr(e), CalcLexer.COMMA(_), ArgList(rest)) => e :: rest },
    { case Expr(e) => List(e) }
  )
  
  val FunctionCall: Rule[(String, Option[List[Int]])] = rule(
    { case (CalcLexer.ID(name), CalcLexer.LPAREN(_), ArgList.Option(args), CalcLexer.RPAREN(_)) =>
      (name.value, args)
    }
  )
  
  val root = rule { case FunctionCall(fc) => fc }
}

// Parse: f()
// Result: ("f", None)

// Parse: f(1, 2, 3)
// Result: ("f", Some(List(1, 2, 3)))
```

**EBNF extensions:**
- `Rule.Option` - Matches 0 or 1 occurrence (like `?` in EBNF)
- `Rule.List` - Matches 0 or more occurrences (like `*` in EBNF)

### 3. Naming Productions

Use `@name` to label productions for conflict resolution:

```scala
val Expr: Rule[Int] = rule(
  { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b }: @name("addition"),
  { case (Expr(a), CalcLexer.TIMES(_), Expr(b)) => a * b }: @name("multiplication")
)
```

### 4. Context-Aware Parsing

Access and modify parser context during parsing:

```scala
case class MyContext(
  var depth: Int = 0,
  symbols: mutable.Set[String] = mutable.Set.empty
) extends GlobalCtx

object ContextParser extends Parser[MyContext] {
  val Block: Rule[Unit] = rule(
    { case (CalcLexer.LBRACE(_), Statements(_), CalcLexer.RBRACE(_)) =>
      ctx.depth += 1
      ()
    }
  )
}
```

### 5. Semantic Actions

Execute arbitrary code in production actions:

```scala
val Statement: Rule[Int] = rule(
  { case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(value)) =>
    println(s"Assigning ${value} to ${id.value}")
    ctx.names(id.value) = value
    value
  }
)
```

### 6. Error Handling

ALPACA parsers return `null` on parse errors:

```scala
val tokens = CalcLexer.tokenize("invalid syntax")
val (ctx, result) = CalcParser.parse[Int](tokens)

if (result == null) {
  println("Parse error!")
} else {
  println(s"Result: $result")
}
```

## Complete Working Example: Calculator Language

Here's a comprehensive parser for a calculator with functions and precedence:

```scala
import alpaca.api.*
import alpaca.parser.Production as P

val CalcLexer = lexer {
  case " " => Token.Ignored
  case "\\t" => Token.Ignored
  case number @ "[0-9]+" => Token["NUMBER"](number.toInt)
  case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
  case "\\+" => Token["PLUS"]
  case "-" => Token["MINUS"]
  case "\\*" => Token["TIMES"]
  case "/" => Token["DIVIDE"]
  case "\\*\\*" => Token["POWER"]
  case "=" => Token["ASSIGN"]
  case "\\(" => Token["LPAREN"]
  case "\\)" => Token["RPAREN"]
  case "," => Token["COMMA"]
}

case class CalcContext(
  names: mutable.Map[String, Int] = mutable.Map.empty
) extends GlobalCtx

object CalcParser extends Parser[CalcContext] {
  // Expressions with full precedence
  val Expr: Rule[Int] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Expr(b)) => a + b }: @name("plus"),
    { case (Expr(a), CalcLexer.MINUS(_), Expr(b)) => a - b }: @name("minus"),
    { case (Expr(a), CalcLexer.TIMES(_), Expr(b)) => a * b }: @name("times"),
    { case (Expr(a), CalcLexer.DIVIDE(_), Expr(b)) => a / b }: @name("divide"),
    { case (Expr(a), CalcLexer.POWER(_), Expr(b)) => math.pow(a, b).toInt }: @name("power"),
    { case (CalcLexer.MINUS(_), Expr(e)) => -e }: @name("uminus"),
    { case (CalcLexer.LPAREN(_), Expr(e), CalcLexer.RPAREN(_)) => e },
    { case CalcLexer.NUMBER(n) => n.value },
    { case CalcLexer.ID(id) => ctx.names.getOrElse(id.value, 0) }
  )
  
  // Argument list for function calls
  val ArgList: Rule[List[Int]] = rule(
    { case (Expr(e), CalcLexer.COMMA(_), ArgList(rest)) => e :: rest },
    { case Expr(e) => List(e) }
  )
  
  // Function call or variable reference
  val Call: Rule[Int] = rule(
    { case (CalcLexer.ID(name), CalcLexer.LPAREN(_), ArgList.Option(args), CalcLexer.RPAREN(_)) =>
      // Simple built-in functions
      (name.value, args) match {
        case ("sum", Some(values)) => values.sum
        case ("max", Some(values)) => values.max
        case ("min", Some(values)) => values.min
        case _ => 0
      }
    }
  )
  
  // Statements: assignments or expressions
  val Statement: Rule[Int] = rule(
    { case (CalcLexer.ID(id), CalcLexer.ASSIGN(_), Expr(value)) =>
      ctx.names(id.value) = value
      value
    },
    { case Expr(e) => e },
    { case Call(c) => c }
  )
  
  val root = rule { case Statement(result) => result }
  
  // Resolve conflicts to define precedence and associativity
  override val resolutions = Set(
    // Power has highest precedence, right-associative
    P.ofName("power").before(CalcLexer.POWER),
    P.ofName("power").before(CalcLexer.TIMES, CalcLexer.DIVIDE, CalcLexer.PLUS, CalcLexer.MINUS),
    
    // Unary minus
    P.ofName("uminus").before(CalcLexer.TIMES, CalcLexer.DIVIDE, CalcLexer.PLUS, CalcLexer.MINUS),
    
    // Multiplication and division
    P.ofName("times").before(CalcLexer.TIMES, CalcLexer.DIVIDE),
    P.ofName("times").before(CalcLexer.PLUS, CalcLexer.MINUS),
    P.ofName("divide").before(CalcLexer.TIMES, CalcLexer.DIVIDE),
    P.ofName("divide").before(CalcLexer.PLUS, CalcLexer.MINUS),
    
    // Addition and subtraction (lowest precedence)
    P.ofName("plus").after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    P.ofName("plus").before(CalcLexer.PLUS, CalcLexer.MINUS),
    P.ofName("minus").after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    P.ofName("minus").before(CalcLexer.PLUS, CalcLexer.MINUS)
  )
}

// Use the parser
val input1 = "2 + 3 * 4"
val tokens1 = CalcLexer.tokenize(input1)
val (_, result1) = CalcParser.parse[Int](tokens1)
println(result1)  // 14

val input2 = "x = 10"
val tokens2 = CalcLexer.tokenize(input2)
val (ctx2, result2) = CalcParser.parse[Int](tokens2)
println(result2)  // 10
println(ctx2.names("x"))  // 10

val input3 = "sum(1, 2, 3)"
val tokens3 = CalcLexer.tokenize(input3)
val (_, result3) = CalcParser.parse[Int](tokens3)
println(result3)  // 6
```

## Advanced Features

### LR Parse Table Generation

ALPACA automatically generates LR parse tables at compile time:
- Constructs the LR(0) automaton
- Builds ACTION and GOTO tables
- Detects shift-reduce and reduce-reduce conflicts
- Provides conflict resolution through precedence declarations

### Type Safety

ALPACA ensures type safety throughout:
```scala
val Expr: Rule[Int] = rule(...)
val Statement: Rule[Unit] = rule(...)

// This won't compile - type mismatch
val BadRule: Rule[String] = rule(
  { case Expr(n) => n }  // Error: Int doesn't match String
)
```

### Compile-Time Validation

Parse tables are generated during compilation:
- Grammar conflicts detected at compile time
- Invalid rule references cause compilation errors
- Type mismatches in productions caught early
