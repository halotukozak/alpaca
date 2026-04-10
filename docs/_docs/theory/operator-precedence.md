# Operator Precedence Grammars

When a grammar has binary operators at multiple precedence levels, the parser must decide which operations to perform first. There are two approaches: encode precedence in the grammar structure (stratification), or use a flat grammar with explicit conflict resolution. Alpaca supports both.

## The Problem

The expression `1 + 2 * 3` has two valid parse trees:

```
    +               *
   / \             / \
  1   *           +   3
     / \         / \
    2   3       1   2

  = 1 + 6 = 7   = 3 * 3 = 9
```

The grammar `Expr → Expr + Expr | Expr * Expr | NUMBER` is ambiguous — it does not specify which tree to build. Precedence rules resolve this: `*` binds tighter than `+`, so `1 + 2 * 3 = 7`.

## Approach 1: Grammar Stratification

Encode precedence by splitting `Expr` into layers, one per precedence level:

```
Expr   → Expr + Term | Expr - Term | Term
Term   → Term * Factor | Term / Factor | Factor
Factor → ( Expr ) | NUMBER
```

This grammar is **unambiguous**. The structure forces `*` and `/` to bind tighter than `+` and `-`: a `Term` must be fully reduced before it participates in an `Expr`-level operation.

In Alpaca:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Double] = rule(
    { case (Expr(a), CalcLexer.PLUS(_), Term(b))  => a + b },
    { case (Expr(a), CalcLexer.MINUS(_), Term(b)) => a - b },
    { case Term(t) => t },
  )
  val Term: Rule[Double] = rule(
    { case (Term(a), CalcLexer.TIMES(_), Factor(b)) => a * b },
    { case (Term(a), CalcLexer.DIVIDE(_), Factor(b)) => a / b },
    { case Factor(f) => f },
  )
  val Factor: Rule[Double] = rule(
    { case CalcLexer.NUMBER(n) => n.value },
    { case (CalcLexer.LPAREN(_), Expr(e), CalcLexer.RPAREN(_)) => e },
  )
  val root: Rule[Double] = rule:
    case Expr(e) => e
```

No `resolutions` needed — the grammar itself is unambiguous.

## Approach 2: Flat Grammar + Conflict Resolution

Keep all operators at the same grammar level and use `before`/`after` to declare precedence:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Double] = rule(
    "plus"  { case (Expr(a), CalcLexer.PLUS(_), Expr(b))  => a + b },
    "minus" { case (Expr(a), CalcLexer.MINUS(_), Expr(b)) => a - b },
    "times" { case (Expr(a), CalcLexer.TIMES(_), Expr(b))  => a * b },
    "div"   { case (Expr(a), CalcLexer.DIVIDE(_), Expr(b)) => a / b },
    { case CalcLexer.NUMBER(n) => n.value },
    { case (CalcLexer.LPAREN(_), Expr(e), CalcLexer.RPAREN(_)) => e },
  )
  val root: Rule[Double] = rule:
    case Expr(e) => e

  override val resolutions = Set(
    production.times.before(CalcLexer.TIMES, CalcLexer.DIVIDE),
    production.div.before(CalcLexer.TIMES, CalcLexer.DIVIDE),
    production.plus.before(CalcLexer.PLUS, CalcLexer.MINUS),
    production.minus.before(CalcLexer.PLUS, CalcLexer.MINUS),
    production.plus.after(CalcLexer.TIMES, CalcLexer.DIVIDE),
    production.minus.after(CalcLexer.TIMES, CalcLexer.DIVIDE),
  )
```

The grammar is ambiguous, but the resolutions tell the parser exactly how to handle every conflict.

## Trade-offs

| | Stratified grammar | Flat grammar + resolution |
|-|-|-|
| **Grammar size** | More non-terminals (one per level) | Fewer non-terminals, more resolution rules |
| **Ambiguity** | None — grammar is unambiguous | Ambiguous — conflicts resolved by declarations |
| **Adding operators** | Requires restructuring non-terminal hierarchy | Add a production and a resolution rule |
| **Readability** | Precedence visible in grammar structure | Precedence visible in resolution declarations |
| **Error messages** | Cleaner — no conflicts to report | Conflict errors until all resolutions are added |

For grammars with 2-3 precedence levels, stratification is straightforward. For grammars with many levels (C has 15), flat grammar + resolution is more maintainable.

## BrainFuck: No Precedence Needed

The BrainFuck grammar has no binary operators — all operations are statements, not expressions. There is no precedence hierarchy and no ambiguity:

```
root      → Operation*
Operation → next | prev | inc | dec | print | read | While | FunctionDef | FunctionCall
While     → [ Operation* ]
```

Every token uniquely determines which rule to apply. The parser never faces a shift/reduce choice. This is why the BrainFuck parser needs no `resolutions`.

## Cross-links

- See [Conflict Resolution](../conflict-resolution.md) for the full `before`/`after` DSL reference.
- See [Conflicts and Disambiguation](conflicts.md) for the formal theory of shift/reduce and reduce/reduce conflicts.
- See the [Expression Evaluator](../cookbook/expression-evaluator.md) for a complete calculator with precedence resolution.
