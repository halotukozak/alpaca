# Conflict Resolution

LR(1) parsing is deterministic: the parser always knows exactly what to do next — or it reports a conflict at compile time.
A conflict arises when the grammar is ambiguous in a way the LR algorithm cannot resolve on its own.
Alpaca reports conflicts as compile errors and gives you the `before`/`after` DSL to resolve them by declaring explicit precedence relationships between productions and tokens.

> **Compile-time processing:** When you define `override val resolutions = Set(...)`, the Alpaca macro incorporates your precedence declarations into the LR(1) parse table at compile time. Conflicts (`ShiftReduceConflict`, `ReduceReduceConflict`) and cycles (`InconsistentConflictResolution`) are detected and reported as compile errors. At runtime, the resolved parse table executes deterministically -- no conflict checking happens during parsing.

## Understanding Conflicts

Two types of conflicts can occur:

**Shift/reduce conflict:** the parser has shifted several tokens onto its stack and matched a complete production's right-hand side.
It now has two valid choices: *reduce* (apply the production to produce a non-terminal value) or *shift* (push the next token and keep going).
An arithmetic grammar with `1 + 2 + 3` demonstrates this: after matching `1 + 2`, the parser can reduce `1 + 2` to `Expr`, or it can shift the second `+` and keep accumulating.

**Reduce/reduce conflict:** two different productions can reduce the exact same token sequence.
For example, if `Integer -> Num` and `Float -> Num` are both productions and the parser has `Num` on the stack with no further lookahead to distinguish them, it cannot decide which reduction to apply.

Both conflict types are detected at compile time when the LR(1) parse table is built.
They do not manifest as runtime errors.

## Reading the Error Messages

When a conflict occurs, the Alpaca compiler prints a message identifying the conflicting actions and a representative situation.

Shift/reduce conflict message:

```
Shift "+" vs Reduce Expr -> Expr + Expr
In situation like:
Expr + Expr + ...
Consider marking production Expr -> Expr + Expr to be alwaysBefore or alwaysAfter "+"
```

Reduce/reduce conflict message:

```
Reduce Integer -> Num vs Reduce Float -> Num
In situation like:
Num ...
Consider marking one of the productions to be alwaysBefore or alwaysAfter the other
```

> **Known issue: error messages suggest `alwaysBefore`/`alwaysAfter`.** The compile error text says _"Consider marking production X to be alwaysBefore or alwaysAfter Y"_. These method names do **not** exist in the Alpaca API. The correct methods are `before` and `after`. This is a known discrepancy between the error message text and the public API.
>
> When you see this message, use `before` and `after` — not `alwaysBefore` or `alwaysAfter`.

[//]: # (todo: fix these names in API)

Concretely — the error message and its fix:

```
Error: Consider marking production Expr -> Expr + Expr to be alwaysBefore or alwaysAfter "+"
Fix:   production.plus.before(Lexer.PLUS)
```

## LR Parsing Basics

Just enough to reason about conflicts:

- **Shift** — push the next input token onto the parse stack and advance past it.
- **Reduce** — pop symbols off the stack matching a production's right-hand side, then push the non-terminal that production produces.
- **Conflict** — the parser is in a state where both shift and reduce (or two different reductions) are valid. The parse table has more than one action for the same state and lookahead token.
- **Resolution** — a priority rule that tells the parser which action to prefer when a conflict exists.

The LR algorithm processes input left-to-right with one token of lookahead (hence LR(1)).
When you declare `production.plus.before(Lexer.PLUS)`, you tell the algorithm: in the conflict state, prefer reducing the `plus` production over shifting the `+` token.

## Naming Productions

To reference a production in `resolutions`, give it a name by placing a string literal directly before the `{ case ... }` block:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Int] = rule(
    "plus"  { case (Expr(a), Lexer.PLUS(_), Expr(b))  => a + b },
    "minus" { case (Expr(a), Lexer.MINUS(_), Expr(b)) => a - b },
    "times" { case (Expr(a), Lexer.TIMES(_), Expr(b)) => a * b },
    { case Lexer.NUMBER(n) => n.value },   // unnamed
  )
  val root = rule:
    case Expr(e) => e
```

Rules:
- The name must be a string literal placed immediately before the brace (not a `val`, not a computed value).
- Not all productions need names — only those you reference in `resolutions`.
- If you reference an undefined name (e.g., `production.typo`), the compiler reports: _"Production with name 'typo' not found"_.

## The before/after DSL

Three resolution forms are available:

**1. `production.name.before(tokens*)`** — this production takes precedence; prefer reducing this production over shifting any of the listed tokens.

**2. `production.name.after(tokens*)`** — those tokens take precedence; prefer shifting those tokens over reducing this production.

**3. `Token.before(productions*)`** — shifting this token takes precedence over reducing any of the listed productions (the token-side spelling of the same rule).

[//]: # (todo: da się też ustawiać produkcje w kolejności)

Full `resolutions` example for a calculator with `+`, `-`, `*`, `/`:

```scala sc:nocompile
import alpaca.*

override val resolutions = Set(
  production.plus.before(Lexer.PLUS, Lexer.MINUS),
  production.plus.after(Lexer.TIMES, Lexer.DIVIDE),
  production.minus.before(Lexer.PLUS, Lexer.MINUS),
  production.minus.after(Lexer.TIMES, Lexer.DIVIDE),
)
```

Reading `production.plus.before(Lexer.PLUS, Lexer.MINUS)`:
when the parser has reduced the `plus` production and the next token is `+` or `-`, prefer the reduction — do not shift.
This gives `+` left associativity and equal precedence with `-`.

**Transitivity:** `before`/`after` constraints are transitive.
If A is before B and B is before C, then A is implicitly before C.
This is how hierarchical precedence works: you only need to state the direct relationships, and the compiler derives the full order.

[//]: # (todo: Transitivity should be e bigger subsection)

**Note:** `production` is a `@compileTimeOnly` compile-time construct — it is only valid inside the `resolutions` value.
Using it anywhere else (in a rule body, in a companion, in a method) is a compile error.

## The Production(symbols*) Selector

For productions that are not named, use `Production(symbols*)` to identify them by their exact right-hand side:

```scala sc:nocompile
import alpaca.*
import alpaca.Production as P

override val resolutions = Set(
  P(Expr, Lexer.TIMES, Expr).before(Lexer.PLUS, Lexer.MINUS),
  P(Lexer.MINUS, Expr).before(Lexer.PLUS, Lexer.MINUS),
)
```

Arguments to `Production(...)` are `Rule` references and `Token` references that exactly match the right-hand side of the production you want to select.
If no production has that exact sequence, the compiler reports: _"Production with RHS '...' not found"_.

Both styles — `production.name` and `Production(symbols*)` — can coexist in one `resolutions` set:

```scala sc:nocompile
import alpaca.*
import alpaca.Production as P

override val resolutions = Set(
  P(Expr, Lexer.TIMES, Expr).before(Lexer.DIVIDE, Lexer.TIMES, Lexer.PLUS, Lexer.MINUS),
  production.plus.before(Lexer.PLUS, Lexer.MINUS),
  production.plus.after(Lexer.TIMES, Lexer.DIVIDE),
  production.minus.before(Lexer.PLUS, Lexer.MINUS),
  production.minus.after(Lexer.TIMES, Lexer.DIVIDE),
)
```

## Token-Side Resolution

`before` and `after` can also be called directly on a token.
This is the token-side spelling: "shifting this token wins over reducing those productions."

```scala sc:nocompile
import alpaca.*

override val resolutions = Set(
  Lexer.exp.before(
    production.uplus,
    production.uminus,
    production.mod,
  ),
)
```

`Lexer.exp.before(production.uplus, ...)` means: when the conflict is between shifting `exp` and reducing any of those productions, shift.
This is equivalent to `production.uplus.after(Lexer.exp)` — choose whichever spelling reads more naturally for the case at hand.

## Associativity

Associativity is a special case of precedence where the conflict involves the same operator on both sides.

**Left-associative** (`1 + 2 + 3 = (1 + 2) + 3`): prefer reducing before shifting the same operator.

```scala sc:nocompile
import alpaca.*

production.plus.before(Lexer.PLUS)  // same operator: reduce first
```

After reducing `1 + 2` to `Expr`, the next token is `+`.
`before(Lexer.PLUS)` says: prefer reducing (so `1 + 2` becomes an `Expr`), then shift the second `+`.
Result: left grouping.

**Right-associative** (`a = b = c` means `a = (b = c)`): prefer shifting before reducing.

```scala sc:nocompile
import alpaca.*

production.assign.after(Lexer.ASSIGN)  // same operator: shift first (right-associative)
```

After matching `a =`, the parser shifts the next `b` and `=` before reducing `b = c`.
Result: right grouping.

## Conflict Cycle Detection

The compiler detects cycles in the transitive closure of `before`/`after` constraints.
A cycle like A before B before C before A is contradictory — the compiler cannot build a consistent priority table.
This error is reported as an `InconsistentConflictResolution` compile-time exception.

The `InconsistentConflictResolution` error message:

```
Inconsistent conflict resolution detected:
Reduction(A) before Shift(+) before Reduction(+ -> B) before Reduction(A)
There are elements being both before and after Reduction(A) at the same time.
Consider revising the before/after rules to eliminate cycles
```

The message shows the full cycle path.

[//]: # (todo: sprwadzic czy to prawda, jeśli nie to sprawić, żeby było)

## Ordering Constraint

`resolutions` must be the **last `val`** in the parser object.

The macro reads parser class declarations top-to-bottom.
When it processes `resolutions`, it must already have seen every rule declaration — otherwise `production.name` references fail with _"Production with name X not found"_ because the named production has not been registered yet.

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Int] = rule(         // rules first
    "plus" { case (Expr(a), Lexer.PLUS(_), Expr(b)) => a + b },
    { case Lexer.NUMBER(n) => n.value },
  )
  val root = rule:                    // root rule second
    case Expr(e) => e

  override val resolutions = Set(     // resolutions last
    production.plus.before(Lexer.PLUS),
  )
```

Placing `resolutions` before the rule declarations is the most common source of _"Production with name X not found"_ errors.

---

See [Parser](parser.html) for grammar rules, EBNF operators, and parsing input.
