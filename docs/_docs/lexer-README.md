# Alpaca Lexer Documentation

Welcome to the Alpaca lexer documentation! This guide helps you navigate the complete lexer documentation and find what you need.

## Quick Links

| Document | For | Duration |
|----------|-----|----------|
| [Lexer Quickstart](#quickstart) | Copy-paste examples and common patterns | 15 min |
| [Lexer Development Guide](#development-guide) | Comprehensive API and usage reference | 45 min |
| [Lexer API Reference](#api-reference) | Type signatures and quick lookup | 10 min |
| [Lexer Internals](#internals) | Macro implementation and customization | 60 min |

---

## Which Document Should I Read?

### I'm new to Alpaca

✅ Start here: **[Getting Started](./getting-started.md)**

Then move to: **[Lexer Quickstart](./lexer-quickstart.md)**

### I want to build a lexer

✅ Read: **[Lexer Quickstart](./lexer-quickstart.md)** (examples)

Then: **[Lexer Development Guide](./lexer-development.md)** (reference)

Keep handy: **[Lexer API Reference](./lexer-api-reference.md)** (lookup)

### I need to understand how it works

✅ Read: **[Lexer Internals](./lexer-internals.md)** (macro, types, runtime)

Then: **[Lexer Development Guide](./lexer-development.md)** (architecture overview)

### I'm extending the lexer

✅ Read: **[Lexer Internals](./lexer-internals.md)** (deep dive)

Reference: **[Lexer API Reference](./lexer-api-reference.md)** (type classes)

### I have a specific question

🔍 Use the table below:

| Question | Document |
|----------|----------|
| How do I define tokens? | [Quickstart: Hello World](./lexer-quickstart.md#hello-world-a-simple-lexer) |
| How do I handle keywords? | [Quickstart: Common Patterns](./lexer-quickstart.md#pattern-keyword-recognition) |
| How do I track state? | [Quickstart: Stateful Lexer](./lexer-quickstart.md#stateful-lexer-tracking-nesting) or [Development: Context](./lexer-development.md#lexer-context) |
| What's the token API? | [API Reference: Token DSL](./lexer-api-reference.md#token-dsl) |
| How does context work? | [Development: Lexer Context](./lexer-development.md#lexer-context) |
| How do I test a lexer? | [Development: Testing](./lexer-development.md#testing-lexers) |
| How are patterns matched? | [Internals: Runtime Execution](./lexer-internals.md#runtime-execution-model) |
| How do macros work? | [Internals: Macro Expansion](./lexer-internals.md#macro-expansion-pipeline) |
| How do I customize it? | [Internals: Extending](./lexer-internals.md#extending-the-lexer) |

---

## Documentation Structure

### Quickstart

**File:** [lexer-quickstart.md](./lexer-quickstart.md)

**Best for:** Getting hands-on quickly with copy-paste examples.

**Covers:**
- Hello World lexer
- Math expression lexer
- Programming language lexer
- Stateful lexer (nesting depth)
- Common patterns (keywords, numbers, strings, operators)
- Tips and tricks

**Time:** 15–20 minutes

**When to use:**
- You're new to lexers
- You want practical examples
- You need a starting template

### Development Guide

**File:** [lexer-development.md](./lexer-development.md)

**Best for:** Comprehensive understanding of the lexer system.

**Covers:**
- Overview and design philosophy
- Public API (the `lexer` macro)
- Token DSL (Ignored, basic, with value)
- Lexer context (built-in, custom)
- Runtime tokenization
- Context manipulation
- Adding/modifying tokens
- Architecture overview
- Testing patterns
- Development checklist

**Time:** 45–60 minutes

**When to use:**
- You want to understand the whole system
- You're designing a new language
- You need to make design decisions

### API Reference

**File:** [lexer-api-reference.md](./lexer-api-reference.md)

**Best for:** Quick lookup of types and method signatures.

**Covers:**
- `lexer` macro signature
- `Token` factory methods
- Context types (`Default`, `Empty`, custom)
- `Lexeme` and `TokenInfo` structures
- Type classes (`Copyable`, `BetweenStages`, `LexerRefinement`)
- Position and line tracking traits
- Common patterns (keywords, numbers, strings, operators)
- Error handling
- Testing utilities
- Debugging aids

**Time:** 5–10 minutes (per lookup)

**When to use:**
- You know what you want but need the exact type signature
- You need to remember a method name
- You're writing a custom type class

### Internals

**File:** [lexer-internals.md](./lexer-internals.md)

**Best for:** Understanding implementation details and extending the system.

**Covers:**
- Macro expansion pipeline (5 stages)
- Type-level architecture
- Runtime execution model
- Internal type classes and their behavior
- LazyReader and string scanning
- Copyable type class implementation
- Extending the lexer (custom contexts, BetweenStages)
- Performance considerations
- Debugging techniques

**Time:** 60–90 minutes

**When to use:**
- You're contributing to Alpaca
- You need custom `BetweenStages` or `Copyable`
- You want to optimize lexing
- You're curious about the implementation

---

## Common Tasks

### Define a Basic Lexer

**See:** [Quickstart: Hello World](./lexer-quickstart.md#hello-world-a-simple-lexer)

### Define Tokens with Values

**See:** [Quickstart: Math Lexer](./lexer-quickstart.md#math-expression-lexer)

### Handle Keywords vs Identifiers

**See:** [Quickstart: Programming Language Lexer](./lexer-quickstart.md#programming-language-lexer) or [Common Patterns: Keyword Recognition](./lexer-quickstart.md#pattern-keyword-recognition)

### Parse Numbers (int, float, hex)

**See:** [Quickstart: Common Patterns - Numeric Literals](./lexer-quickstart.md#pattern-numeric-literals)

### Handle Strings with Escapes

**See:** [Quickstart: Common Patterns - String Literals](./lexer-quickstart.md#pattern-string-literals)

### Track Line Numbers

**See:** [Development: LexerCtx.Default](./lexer-development.md#lexerctxdefault)

### Implement Custom State (Nesting, Indentation)

**See:** [Quickstart: Stateful Lexer](./lexer-quickstart.md#stateful-lexer-tracking-nesting) or [Development: Custom Contexts](./lexer-development.md#custom-contexts)

### Test a Lexer

**See:** [Development: Testing Lexers](./lexer-development.md#testing-lexers)

### Handle Comments

**See:** [Quickstart: Common Patterns - Comments](./lexer-quickstart.md#pattern-comments)

### Resolve Token Ambiguities

**See:** [Development: Adding and Modifying Tokens](./lexer-development.md#adding-and-modifying-tokens)

### Debug Pattern Matching

**See:** [Quickstart: Debugging](./lexer-quickstart.md#debugging-lexer-issues)

---

## Examples by Language Type

### Simple Expression Lexer

**See:** [Quickstart: Math Expression Lexer](./lexer-quickstart.md#math-expression-lexer)

**Covers:** Numbers, operators, grouping, whitespace

### Programming Language Lexer

**See:** [Quickstart: Programming Language Lexer](./lexer-quickstart.md#programming-language-lexer)

**Covers:** Keywords, identifiers, operators, strings, comments, numbers

### Stateful Language Lexer

**See:** [Quickstart: Stateful Lexer](./lexer-quickstart.md#stateful-lexer-tracking-nesting)

**Covers:** Context tracking, error accumulation, bracket matching

### Indentation-Sensitive Language

**See:** [Internals: Custom Context - Advanced State](./lexer-internals.md#custom-context-with-advanced-state-tracking)

**Covers:** Indentation stack, custom context, advanced state

---

## Key Concepts at a Glance

### Token

A token is a recognized element of the input:

```scala
case "[0-9]+" => Token["int"](_.toInt)     // Token with value
case "#.*" => Token.Ignored                 // Token without output
```

**Learn more:** [Development: Token DSL](./lexer-development.md#token-dsl)

### Pattern

A regular expression that matches a token:

```scala
case "[0-9]+" => ...         // Matches one or more digits
case "[a-zA-Z_][\\w]*" => ...  // Matches identifiers
```

**Learn more:** [Development: Pattern Syntax](./lexer-development.md#pattern-syntax)

### Context

Stateful information during lexing:

```scala
case class MyCtx(
  var text: CharSequence = "",
  var line: Int = 1,
) extends LexerCtx

val Lexer = lexer[MyCtx] { ... }
```

**Learn more:** [Development: Lexer Context](./lexer-development.md#lexer-context)

### Lexeme

A matched token with its value:

```scala
Lexeme("int", 123, contextSnapshot)
```

**Learn more:** [API Reference: Lexeme](./lexer-api-reference.md#lexeme)

---

## Troubleshooting

### Issue: Token not recognized

**Solution:** Check pattern order and verify regex is correct

**See:** [Quickstart: Debugging](./lexer-quickstart.md#debugging-lexer-issues) or [Development: Precedence](./lexer-development.md#workflow-changing-token-precedence)

### Issue: Wrong token matched

**Solution:** More specific patterns must come first

**See:** [Development: Pattern Ordering](./lexer-development.md#pattern-syntax)

### Issue: Type mismatch in value extraction

**Solution:** Ensure value type matches the extraction function

**See:** [Quickstart: Common Issues](./lexer-quickstart.md#issue-type-mismatch-in-value-extraction)

### Issue: Lexing is slow

**Solution:** Optimize pattern order, use LazyReader internally

**See:** [Internals: Performance](./lexer-internals.md#performance-considerations)

---

## Learning Path

Follow this progression based on your goals:

### Path 1: Just Want to Use It

1. [Getting Started](./getting-started.md) — 5 min
2. [Lexer Quickstart: Hello World](./lexer-quickstart.md#hello-world-a-simple-lexer) — 5 min
3. [Lexer Quickstart: Examples](./lexer-quickstart.md#math-expression-lexer) — 10 min
4. **Start coding!**
5. Reference [API Reference](./lexer-api-reference.md) as needed

**Total:** 20 min

### Path 2: Want to Build Language Lexers

1. [Getting Started](./getting-started.md) — 5 min
2. [Lexer Quickstart](./lexer-quickstart.md) — 20 min
3. [Lexer Development Guide](./lexer-development.md) — 45 min
4. [Lexer API Reference](./lexer-api-reference.md) — 5 min
5. **Start designing!**

**Total:** 75 min

### Path 3: Want to Understand Deeply

1. [Getting Started](./getting-started.md) — 5 min
2. [Lexer Quickstart](./lexer-quickstart.md) — 20 min
3. [Lexer Development Guide](./lexer-development.md) — 45 min
4. [Lexer Internals](./lexer-internals.md) — 60 min
5. [Lexer API Reference](./lexer-api-reference.md) — 5 min
6. **Read the source code**
7. **Contribute!**

**Total:** 135 min

---

## Document Cross-References

All documents are hyperlinked. Use these shortcuts:

- **Table of Contents** at the top of each document
- **See Also** sections at the end
- **Internal links** throughout for related topics
- **Index below** for quick navigation

---

## Full Index

### By Topic

- **API**: [API Reference](./lexer-api-reference.md)
- **Context**: [Development: Lexer Context](./lexer-development.md#lexer-context)
- **Examples**: [Quickstart: Examples](./lexer-quickstart.md)
- **Getting Started**: [Getting Started](./getting-started.md)
- **Internals**: [Internals](./lexer-internals.md)
- **Keywords**: [Quickstart: Pattern - Keywords](./lexer-quickstart.md#pattern-keyword-recognition)
- **Numbers**: [Quickstart: Pattern - Numbers](./lexer-quickstart.md#pattern-numeric-literals)
- **Operators**: [Quickstart: Pattern - Operators](./lexer-quickstart.md#pattern-operators-by-precedence)
- **Strings**: [Quickstart: Pattern - Strings](./lexer-quickstart.md#pattern-string-literals)
- **Testing**: [Development: Testing](./lexer-development.md#testing-lexers)
- **Tokens**: [Development: Token DSL](./lexer-development.md#token-dsl)

### By Document

- [Getting Started](./getting-started.md)
- [Lexer Quickstart](./lexer-quickstart.md)
- [Lexer Development Guide](./lexer-development.md)
- [Lexer API Reference](./lexer-api-reference.md)
- [Lexer Internals](./lexer-internals.md)

---

## Feedback and Contributions

Found an error or want to improve the docs? Please [open an issue](https://github.com/halotukozak/alpaca/issues) or [submit a PR](https://github.com/halotukozak/alpaca/pulls).

**Last updated:** December 2025
