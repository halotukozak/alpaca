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

## Zaawansowane aspekty implementacji: Klasy anonimowe i typy rafinowane

### Generacja klas anonimowych w czasie kompilacji

Kluczowym mechanizmem implementacyjnym makra `lexer` jest programatyczna konstrukcja klasy anonimowej w czasie kompilacji. Proces ten wykorzystuje API refleksji TASTy do dynamicznego tworzenia struktur typów, które następnie są materializowane jako kod bajtowy JVM.

#### Konstrukcja symbolu klasy

Anonimowa klasa implementująca `Tokenization[Ctx]` jest tworzona poprzez wywołanie `Symbol.newClass` (linie 201-207 w `Lexer.scala`):

```scala
val cls = Symbol.newClass(
  Symbol.spliceOwner,
  Symbol.freshName("$anon"),
  List(TypeRepr.of[Tokenization[Ctx]]),
  decls,
  None,
)
```

Metoda `Symbol.newClass` przyjmuje następujące parametry:
- **Symbol.spliceOwner** — właściciel nowego symbolu w hierarchii definiowania, zapewniający poprawną widoczność w zakresie leksykalnym
- **Symbol.freshName("$anon")** — generowanie unikalnej nazwy klasy zgodnie z konwencją kompilatatora Scali dla klas anonimowych
- **List(TypeRepr.of[Tokenization[Ctx]])** — lista typów bazowych, w tym przypadku pojedyncza implementacja abstrakcyjnej klasy `Tokenization`
- **decls** — funkcja dostarczająca listy deklaracji członków klasy (pól i metod)
- **None** — opcjonalna klasa companion (niewykorzystana w tym przypadku)

#### Definicja członków klasy

Funkcja `decls` (linie 143-199) konstruuje pełną listę deklaracji dla klasy anonimowej:

1. **Pola tokenów** — dla każdego zdefiniowanego tokena tworzony jest symbol pola typu `DefinedToken[Name, Ctx, Value]`
2. **Type alias Fields** — typ pomocniczy w formie `NamedTuple` ułatwiający strukturalny dostęp do tokenów
3. **Pole compiled** — wartość typu `Regex` zawierająca skompilowane wyrażenie regularne dla wszystkich tokenów
4. **Pole tokens** — lista wszystkich zdefiniowanych tokenów (włączając ignorowane)
5. **Pole byName** — mapa umożliwiająca dynamiczny dostęp do tokenów po nazwie

#### Materiałizacja klasy

Po zdefiniowaniu symbolu klasy, następuje konstrukcja jej ciała (linie 209-255):

```scala
val body = {
  val tokenVals = definedTokens.collect {
    case token: DefinedToken[name, Ctx, value] =>
      ValDef(cls.fieldMember(tokenName), Some(tokenExpr))
  }
  
  tokenVals ++ Vector(
    TypeDef(cls.typeMember("Fields")),
    ValDef(cls.fieldMember("compiled"), Some(regexExpr)),
    ValDef(cls.fieldMember("tokens"), Some(tokensListExpr)),
    ValDef(cls.fieldMember("byName"), Some(byNameMapExpr))
  )
}
```

Klasa jest następnie instancjonowana poprzez wywołanie jej konstruktora (linia 274):

```scala
val newCls = Typed(
  New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone,
  TypeTree.of[refinedTpe]
)
```

### Typy rafinowane (refinement types)

Mechanizm typów rafinowanych stanowi fundamentalną cechę systemu typów Scali umożliwiającą precyzyjne wyrażenie struktury typów w czasie kompilacji. W kontekście implementacji leksera, typy rafinowane pozwalają na dodanie informacji o polach tokenów bezpośrednio do typu zwracanego przez makro.

#### Proces rafinowania typu

Typ wynikowy jest konstruowany poprzez iteracyjne rafinowanie typu bazowego (linie 267-272):

```scala
definedTokens
  .foldLeft(TypeRepr.of[Tokenization[Ctx]]):
    case (tpe, token: DefinedToken[name, Ctx, value]) =>
      Refinement(tpe, tokenName, tokenType)
  .asType match
    case '[refinedTpe] =>
      Block(clsDef :: Nil, newCls).asExprOf[Tokenization[Ctx] & refinedTpe]
```

Funkcja `Refinement(tpe, name, memberType)` tworzy nowy typ będący rozszerzeniem typu `tpe` o członka `name` typu `memberType`. Operacja ta jest wykonywana w czasie kompilacji i nie generuje dodatkowego kodu w czasie wykonania.

#### Typ przecięcia (intersection type)

Wynikowy typ ma formę typu przecięcia:

```
Tokenization[Ctx] & { 
  val TOKEN1: DefinedToken["TOKEN1", Ctx, Type1]
  val TOKEN2: DefinedToken["TOKEN2", Ctx, Type2]
  ...
}
```

Ten typ reprezentuje wartości będące jednocześnie instancjami `Tokenization[Ctx]` oraz posiadające określone pola strukturalne. Kompilator Scali wykorzystuje tę informację do:
- Walidacji dostępu do pól w czasie kompilacji
- Zapewnienia inferowania precyzyjnych typów
- Umożliwienia refaktoryzacji z pełnym wsparciem IDE

### Uzasadnienie wybranego podejścia implementacyjnego

#### Eliminacja narzutu wykonania w czasie działania programu

Wszystkie definicje tokenów są rozwiązywane statycznie w czasie kompilacji. Dostęp do tokenów odbywa się poprzez bezpośrednie odwołanie do pola klasy, co po kompilacji do kodu bajtowego JVM redukuje się do instrukcji `getfield` — operacji o złożoności O(1) bez żadnego narzutu pośrednictwa.

Alternatywne podejście oparte na strukturze mapującej (np. `Map[String, Token]`) wymagałoby:
- Obliczenia funkcji haszującej dla klucza
- Przeszukiwania tablicy haszującej
- Potencjalnej obsługi kolizji
- Dynamicznego rzutowania typu

co wprowadzałoby znaczący narzut wydajnościowy oraz eliminowało możliwość optymalizacji przez kompilator JIT.

#### Bezpieczeństwo typów na poziomie systemu

Dzięki typom rafinowanym, każdy token posiada precyzyjny typ znany kompilatorowi:

```scala
val lexer = lexer {
  case x @ "[0-9]+" => Token["INT"](x.toInt)
  case x @ "[0-9]+\\.[0-9]+" => Token["FLOAT"](x.toDouble)
}

lexer.INT    // Typ: DefinedToken["INT", DefaultGlobalCtx, Int]
lexer.FLOAT  // Typ: DefinedToken["FLOAT", DefaultGlobalCtx, Double]
```

System typów weryfikuje poprawność wszystkich operacji w czasie kompilacji, eliminując możliwość błędów związanych z niepoprawnym typowaniem wartości tokenów.

#### Integracja z narzędziami deweloperskimi

Ponieważ tokeny są reprezentowane jako rzeczywiste pola w typie, środowiska deweloperskie (IDE) mogą wykorzystać informacje typu do:
- Automatycznego uzupełniania nazw tokenów
- Prezentacji pełnych sygnatur typów przy najechaniu kursorem
- Nawigacji do definicji przez mechanizm "go-to-definition"
- Wykrywania błędów składniowych przed kompilacją

Te funkcjonalności są niemożliwe do realizacji w przypadku dostępu przez struktury dynamiczne.

#### Statyczna detekcja konfliktów wzorców

Makro przeprowadza analizę wszystkich wzorców w czasie kompilacji, wykrywając potencjalne konflikty nakładających się wyrażeń regularnych:

```scala
// Kod nie skompiluje się — wykryto konflikt wzorców
val invalid = lexer {
  case "[a-zA-Z_][a-zA-Z0-9_]*" => Token["IDENTIFIER"]
  case "[a-zA-Z]+" => Token["ALPHABETIC"]  // Błąd: wzorzec nakłada się z poprzednim
}
```

Mechanizm ten zapewnia, że błędy konfiguracji są wykrywane na etapie kompilacji, a nie w czasie wykonania programu, co jest zgodne z zasadą "fail-fast" w inżynierii oprogramowania.

#### Typowanie strukturalne z gwarancjami nominalnymi

Zastosowanie typów rafinowanych łączy zalety typowania strukturalnego (elastyczność w dostępie do składowych) z bezpieczeństwem typowania nominalnego (jednoznaczna identyfikacja typów). Każde pole w typie rafinowanym ma precyzyjny typ nominalny, podczas gdy dostęp do tych pól odbywa się przez nazwę, co zapewnia elastyczność interfejsu.

### Analiza alternatywnych rozwiązań

#### Podejście oparte na mapowaniu dynamicznym

```scala
class SimpleLexer {
  val tokens: Map[String, Token[?, ?, ?]] = Map(
    "NUMBER" -> ...,
    "PLUS" -> ...
  )
  def apply(name: String): Token[?, ?, ?] = tokens(name)
}
```

**Wady:**
- Brak bezpieczeństwa typów: błędne nazwy tokenów wykrywane są dopiero w czasie wykonania
- Utrata informacji o typach: zwracany typ to egzystencjalny `Token[?, ?, ?]`
- Narzut wydajnościowy operacji haszowania i przeszukiwania
- Brak wsparcia narzędzi deweloperskich

#### Podejście oparte na jawnej definicji klasy

```scala
class MyLexer extends Tokenization[DefaultGlobalCtx] {
  val NUMBER = DefinedToken[...]
  val PLUS = DefinedToken[...]
  protected def compiled: Regex = "(?<token0>[0-9]+)|(?<token1>\\+)".r
  // ...
}
```

**Wady:**
- Wysoki poziom redundancji kodu (boilerplate)
- Konieczność ręcznej kompilacji wyrażeń regularnych
- Podatność na błędy synchronizacji między definicjami tokenów a wyrażeniem regularnym
- Brak mechanizmu DSL ułatwiającego definicję reguł

### Podsumowanie korzyści

Zastosowanie klas anonimowych i typów rafinowanych w implementacji systemu ALPACA zapewnia:

| Aspekt | Korzyść |
|--------|---------|
| **Wydajność** | Zerowy narzut wykonania, bezpośredni dostęp do pól |
| **Bezpieczeństwo typów** | Pełna weryfikacja w czasie kompilacji |
| **Ergonomia** | Przejrzysty DSL bez kodu pomocniczego |
| **Narzędzia** | Pełne wsparcie IDE i refaktoryzacji |
| **Walidacja** | Statyczna detekcja konfliktów i błędów konfiguracji |
| **Integracja** | Bezproblemowa współpraca z systemem typów Scali |

Zastosowane podejście łączy w sobie zaawansowane techniki metaprogramowania z pragmatycznymi rozwiązaniami inżynierskimi, dostarczając system o wysokim poziomie abstrakcji przy zachowaniu maksymalnej wydajności i bezpieczeństwa.

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
