# Attribute Grammars

An attribute grammar extends a context-free grammar by attaching *attributes* (computed values) to grammar symbols. Each production rule has *semantic rules* that define how attributes flow through the parse tree. Alpaca's semantic actions (`case ... => expression`) are a lightweight form of attribute grammar.

## Synthesized vs Inherited Attributes

**Synthesized attributes** flow bottom-up: a non-terminal's value is computed from its children's values. The parent's attribute depends on its children, not the other way around.

Example: in `Expr → Expr + Term`, the `Expr` on the left gets its value from the `Expr` and `Term` on the right:

```
Expr.val = Expr₁.val + Term.val
```

**Inherited attributes** flow top-down or sideways: a symbol's value depends on its parent or siblings. Example: a type annotation on a declaration propagates down to its initializer.

## S-Attributed Grammars

An *S-attributed grammar* uses only synthesized attributes. Values flow strictly bottom-up — each node computes its value from its children.

Alpaca naturally supports S-attributed grammars. Every `rule` production returns a value computed from the matched children:

```scala sc:nocompile
val Expr: Rule[Double] = rule(
  // Expr.val = Expr₁.val + Term.val (synthesized)
  { case (Expr(a), CalcLexer.PLUS(_), Term(b)) => a + b },
  // Expr.val = Term.val (synthesized)
  { case Term(t) => t },
)
```

The LR parsing algorithm evaluates S-attributed grammars in a single bottom-up pass — exactly how Alpaca's shift/reduce loop works. When a production is reduced, its semantic action runs with the children's values already computed and available.

## L-Attributed Grammars

An *L-attributed grammar* allows inherited attributes, but with a restriction: each attribute of a symbol can depend only on:
- Inherited attributes of the parent
- Attributes of *earlier* (left) siblings in the production

L-attributed grammars can be evaluated in a single left-to-right pass. They are more powerful than S-attributed grammars but require top-down or mixed evaluation.

## Alpaca's ParserCtx as Inherited Attributes

Alpaca does not have formal inherited attributes. Instead, `ParserCtx` provides a shared mutable state that flows through all reductions:

```scala sc:nocompile
case class BrainParserCtx(
  functions: mutable.Set[String] = mutable.Set.empty,
) extends ParserCtx

val FunctionDef: Rule[BrainAST] = rule:
  case (BrainLexer.functionName(name), BrainLexer.functionOpen(_),
        Operation.List(ops), BrainLexer.functionClose(_)) =>
    ctx.functions.add(name.value)   // "inherited" state: writes to shared context
    BrainAST.FunctionDef(name.value, ops)

val FunctionCall: Rule[BrainAST] = rule:
  case (BrainLexer.functionName(name), BrainLexer.functionCall(_)) =>
    ctx.functions.contains(name.value)  // reads from shared context
    BrainAST.FunctionCall(name.value)
```

`ctx.functions` acts like an inherited attribute: information from an earlier reduction (`FunctionDef`) is visible to a later one (`FunctionCall`). But unlike formal inherited attributes:

- The state is **mutable and shared** — all reductions see the same object
- The flow is **order-dependent** — it depends on the sequence of reductions, not the tree structure
- There is **no per-node copying** — changes are global

This is a pragmatic trade-off. For most use cases (symbol tables, function registries, error accumulators), mutable shared state is simpler and faster than formal inherited attributes.

## What Alpaca Supports

| Attribute type | Support | Mechanism |
|---------------|---------|-----------|
| Synthesized | Native | Return values from `rule` bodies |
| Inherited (via ParserCtx) | Practical | Shared mutable state across reductions |
| Inherited (formal, per-node) | Not supported | Would require top-down evaluation |

For purely synthesized computations (calculators, AST construction), Alpaca's `Rule[R]` return values are the right tool. For inherited state (symbol tables, scope tracking), `ParserCtx` provides a workable approximation.

## Cross-links

- See [Semantic Actions](semantic-actions.md) for how Alpaca executes attribute computations during reductions.
- See [Parser Context](../parser-context.md) for the `ParserCtx` API.
- See [AST Construction Patterns](ast-construction.md) for when to compute values inline vs build a tree.
