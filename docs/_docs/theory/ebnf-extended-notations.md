# EBNF and Extended Notations

BNF (Backus-Naur Form) is sufficient to describe any context-free grammar, but common patterns — optional elements, repetition, grouping — require verbose workarounds. EBNF (Extended BNF) adds shorthand for these patterns, and Alpaca's `.Option`, `.List`, and `.SeparatedBy` operators map directly to EBNF concepts.

## BNF vs EBNF

**BNF** uses only: non-terminals, terminals, alternatives (`|`), and concatenation. To express "zero or more Xs" you write explicit recursion:

```
XList → ε
XList → XList X
```

**EBNF** adds three shorthands:

| EBNF | Meaning | BNF equivalent |
|------|---------|---------------|
| `[X]` or `X?` | Optional (zero or one) | `Opt → ε \| X` |
| `{X}` or `X*` | Repetition (zero or more) | `List → ε \| List X` |
| `(A \| B)` | Grouping | Inline alternatives |

EBNF is purely syntactic sugar — it generates the same language as the equivalent BNF, just with less boilerplate.

## Alpaca's EBNF Operators

Alpaca provides three EBNF operators that work on both `Rule[R]` and terminals:

### `.List` — Zero or More

`Rule.List(binding)` matches zero or more occurrences and returns a `List[R]`. The macro generates two synthetic productions (see [Desugaring to Plain BNF](#desugaring-to-plain-bnf) below for the exact expansion).

In Alpaca:

```scala sc:nocompile
val root: Rule[BrainAST] = rule:
  case Operation.List(stmts) => BrainAST.Root(stmts)
  // stmts: List[BrainAST]
```

This is equivalent to the EBNF notation `root → {Operation}`.

### `.Option` — Zero or One

`Rule.Option(binding)` matches zero or one occurrence and returns an `Option[R]`. The macro generates similar synthetic productions (see [Desugaring to Plain BNF](#desugaring-to-plain-bnf) below).

In Alpaca:

```scala sc:nocompile
val root = rule:
  case (Num(n), Num.Option(maybeNum)) =>
    (n, maybeNum)   // maybeNum: Option[Int]
```

This is equivalent to the EBNF notation `root → Num [Num]`.

### `.SeparatedBy` — Zero or More, Separator-Delimited

`Rule.SeparatedBy[Separator](binding)` matches zero or more occurrences delimited by a separator and returns a `List[R | Separator]`. Separators are preserved in the result list (interleaved between the rule values), which is useful when a separator carries its own semantic information (e.g. distinguishing `,` from `;`).

The `Separator` type parameter is a token type (e.g. `MyLexer.`,``) or a rule's singleton type (e.g. `Sep.type`).

```scala sc:nocompile
val root: Rule[List[Any]] = rule:
  case Num.SeparatedBy[MyLexer.`,`](items) => items
  // For "1,2,3": items == List(1, <","-lexeme>, 2, <","-lexeme>, 3)
```

This is equivalent to the EBNF notation `root → [Num {"," Num}]`.

## Desugaring to Plain BNF

Every use of `.List` and `.Option` desugars to plain BNF productions at compile time. The macro generates synthetic non-terminals with fresh names.

For the BrainFuck parser:

```
-- Source Alpaca:
root → Operation.List

-- Desugared BNF:
root         → OperationList
OperationList → ε
OperationList → OperationList Operation
```

For nested EBNF:

```
-- Source Alpaca:
While → jumpForward Operation.List jumpBack

-- Desugared BNF:
While         → jumpForward OperationList jumpBack
OperationList → ε
OperationList → OperationList Operation
```

In practice, the macro generates a fresh synthetic non-terminal (with a randomized name) for each `.List` occurrence. The `OperationList` name above is schematic — the actual generated names are internal.

## When to Use EBNF vs Explicit Recursion

Use `.List` for **unseparated** sequences — elements that follow each other with no delimiter:

```scala sc:nocompile
// Good: BrainFuck operations have no separators
case Operation.List(stmts) => BrainAST.Root(stmts)
```

Use `.SeparatedBy[Sep]` for **separator-delimited** sequences (comma-separated lists, semicolon-separated statements):

```scala sc:nocompile
// Good: JSON members are separated by commas
val ObjectMembers: Rule[List[Any]] = rule:
  case ObjectMember.SeparatedBy[JsonLexer.`,`](members) => members
```

Use **explicit recursion** only when you need to customise the action — for example, dropping separators from the result instead of preserving them, or building a non-`List` shape.

## EBNF in the BrainFuck Grammar

The BrainFuck parser uses `.List` in three places:

| Rule | EBNF equivalent | Purpose |
|------|----------------|---------|
| `root → Operation.List` | `root → {Operation}` | Top-level program: zero or more operations |
| `While → jumpForward Operation.List jumpBack` | `While → "[" {Operation} "]"` | Loop body: zero or more operations |
| `FunctionDef → name "(" Operation.List ")"` | `FunctionDef → name "(" {Operation} ")"` | Function body |

All three expand to the same kind of synthetic list recursion over `Operation`.

## Cross-links

- See [Context-Free Grammars](cfg.md) for the formal BNF notation.
- See [Parser](../parser.md) for the `.List` and `.Option` API reference.
- See [Extractors](../extractors.md) for how to pattern-match on `.List` and `.Option` results.
