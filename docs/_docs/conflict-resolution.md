# Conflict Resolution

LR(1) parsing is deterministic: the parser always knows exactly what to do next -- or it reports a conflict at compile time. A conflict arises when the grammar is ambiguous in a way the LR algorithm cannot resolve on its own. Alpaca gives you the `before`/`after` DSL to resolve conflicts by declaring precedence relationships.

The BrainFuck grammar from [Getting Started](getting-started.md) has no conflicts -- all tokens are unambiguous. This page uses an arithmetic grammar to illustrate conflicts and their resolution.

<details>
<summary>Under the hood: compile-time resolution</summary>

When you define `override val resolutions = Set(...)`, the Alpaca macro incorporates your precedence declarations into the LR(1) parse table at compile time. Conflicts (`ShiftReduceConflict`, `ReduceReduceConflict`) and cycles (`InconsistentConflictResolution`) are detected and reported as compile errors. At runtime, the resolved table executes deterministically.

</details>

## Understanding Conflicts

**Shift/reduce conflict:** the parser has matched a complete production but can also shift the next token. With `1 + 2 + 3`, after matching `1 + 2`, the parser can reduce `1 + 2` to `Expr` or shift the second `+`.

**Reduce/reduce conflict:** two productions can reduce the same token sequence. If `Integer -> Num` and `Float -> Num` are both valid and the parser has `Num` on the stack, it cannot decide which reduction to apply.

Both are detected at compile time. They do not manifest as runtime errors.

## Reading the Error Messages

Shift/reduce conflict:

```
Shift "+" vs Reduce Expr -> Expr + Expr
In situation like:
Expr + Expr + ...
Consider marking production Expr -> Expr + Expr to be before or after "+"
```

The fix:

```
production.plus.before(Lexer.PLUS)
```

## Naming Productions

To reference a production in `resolutions`, name it with a string literal placed before the `{ case ... }` block:

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Int] = rule(
    "plus"  { case (Expr(a), Lexer.PLUS(_), Expr(b))  => a + b },
    "minus" { case (Expr(a), Lexer.MINUS(_), Expr(b)) => a - b },
    "times" { case (Expr(a), Lexer.TIMES(_), Expr(b)) => a * b },
    { case Lexer.NUMBER(n) => n.value },   // unnamed -- not referenced in resolutions
  )
  val root = rule:
    case Expr(e) => e
```

The name must be a string literal placed immediately before the brace. Not all productions need names -- only those you reference in `resolutions`. Referencing an undefined name produces: _"Production with name 'typo' not found"_.

## The before/after DSL

Four resolution forms:

1. **`production.name.before(tokens*)`** -- prefer reducing this production over shifting those tokens.
2. **`production.name.after(tokens*)`** -- prefer shifting those tokens over reducing this production.
3. **`Token.before(productions*)`** -- prefer shifting this token over reducing those productions.
4. **`production.name.before(productions*)`** -- resolve reduce/reduce conflicts between productions.

Full example for a calculator:

```scala sc:nocompile
import alpaca.*

override val resolutions = Set(
  // + and - are left-associative and have equal precedence
  production.plus.before(Lexer.PLUS, Lexer.MINUS),
  production.minus.before(Lexer.PLUS, Lexer.MINUS),
  // * and / bind tighter than + and -
  production.plus.after(Lexer.TIMES, Lexer.DIVIDE),
  production.minus.after(Lexer.TIMES, Lexer.DIVIDE),
)
```

Reading `production.plus.before(Lexer.PLUS, Lexer.MINUS)`: when the parser has reduced `plus` and the next token is `+` or `-`, prefer the reduction. This gives `+` left associativity.

### Transitivity

`before`/`after` constraints are transitive. If A is before B and B is before C, then A is before C. You only state direct relationships; the compiler derives the full order.

## The Production(symbols*) Selector

For unnamed productions, use `Production(symbols*)` to identify them by their right-hand side:

```scala sc:nocompile
import alpaca.*
import alpaca.Production as P

override val resolutions = Set(
  P(Expr, Lexer.TIMES, Expr).before(Lexer.PLUS, Lexer.MINUS),
)
```

Both `production.name` and `Production(symbols*)` can coexist in one `resolutions` set.

## Token-Side Resolution

`before`/`after` can be called on a token directly:

```scala sc:nocompile
import alpaca.*

override val resolutions = Set(
  Lexer.exp.before(production.uplus, production.uminus),
)
```

This is equivalent to `production.uplus.after(Lexer.exp)`. Use whichever reads more naturally.

## Associativity

**Left-associative** (`1 + 2 + 3 = (1 + 2) + 3`): prefer reducing before shifting the same operator.

```scala sc:nocompile
production.plus.before(Lexer.PLUS)   // reduce first -> left grouping
```

**Right-associative** (`a = b = c` groups as `a = (b = c)`): prefer shifting before reducing.

```scala sc:nocompile
production.assign.after(Lexer.ASSIGN)  // shift first -> right grouping
```

## Conflict Cycle Detection

The compiler detects cycles in the transitive closure of constraints. A cycle (A before B before C before A) is contradictory and produces an `InconsistentConflictResolution` error showing the full cycle path.

## Ordering Constraint

`resolutions` must be the **last val** in the parser object. The macro reads declarations top-to-bottom. If `resolutions` appears before a rule declaration, `production.name` references fail with _"Production with name X not found"_.

```scala sc:nocompile
import alpaca.*

object CalcParser extends Parser:
  val Expr: Rule[Int] = rule(           // rules first
    "plus" { case (Expr(a), Lexer.PLUS(_), Expr(b)) => a + b },
    { case Lexer.NUMBER(n) => n.value },
  )
  val root = rule:                      // root second
    case Expr(e) => e

  override val resolutions = Set(       // resolutions LAST
    production.plus.before(Lexer.PLUS),
  )
```

## Best Practices

- **Only resolve actual conflicts.** Add resolutions only for conflicts the compiler reports.
- **Use named productions.** They make resolutions readable and survive refactoring better than `Production(symbols*)`.
- **Think in terms of trees.** "Higher precedence" (`after`) means the operation appears lower in the parse tree -- it binds tighter.

See [Parser](parser.md) for grammar rules and EBNF operators.
