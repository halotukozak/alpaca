# Theory

A tutorial on the compiler-construction theory behind Alpaca: what a lexer and parser actually are, how regular expressions become finite automata, how context-free grammars drive LR parsing, and where conflicts, ambiguity, and error recovery come from.

You don't need any of this to use the library — the [reference pages](../lexer.md) and the [Cookbook](../cookbook/index.md) are enough to build a language. Read this section when you want to understand *why* a grammar has a conflict, what the generated parse table means, or how the pieces fit together formally.

Start with **[The Compilation Pipeline](pipeline.md)** for the mental model everything else builds on, then follow the chapters in order:

- **[The Compilation Pipeline](pipeline.md)** — the four stages from source text to typed result
- **[Tokens and Lexemes](tokens.md)** — token classes vs. instances, and how Alpaca represents them
- **[The Lexer: Regex to Finite Automata](lexer-fa.md)** — compiling regular expressions
- **[Regular vs Context-Free](regular-vs-context-free.md)** — why lexing and parsing are separate problems
- **[Context-Free Grammars](cfg.md)** — productions, derivations, parse trees
- **[EBNF and Extended Notations](ebnf-extended-notations.md)** — repetition and optionality operators
- **[The Shift-Reduce Loop](shift-reduce.md)** — the core parsing algorithm
- **[Why LR Parsing](why-lr.md)** — the trade-offs behind Alpaca's choice
- **[Conflicts and Disambiguation](conflicts.md)** — shift/reduce and reduce/reduce
- **[Semantic Actions](semantic-actions.md)** — attaching meaning to reductions
- **[AST Construction Patterns](ast-construction.md)** — shaping the output tree
- **[Operator Precedence Grammars](operator-precedence.md)** — encoding precedence and associativity
- **[Ambiguity](ambiguity.md)** — grammars with more than one parse
- **[Error Recovery Theory](error-recovery-theory.md)** — continuing past a syntax error
- **[Attribute Grammars](attribute-grammars.md)** — the formal model behind semantic actions
- **[Full Calculator Example](full-example.md)** — every concept in one worked grammar
