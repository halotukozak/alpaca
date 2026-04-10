package alpaca

import alpaca.internal.*

/**
 * Defines an opaque type `Token` that represents a token used in a lexer.
 *
 * This type has three type parameters:
 * - `Name`: The type of the token's name, restricted to a subtype of `ValidName`.
 * - `Ctx`: The type of the lexer context, restricted to a subtype of `LexerCtx`.
 * - `Value`: The type of the token's value.
 *
 * The exact implementation details of the underlying type are abstracted away by using `Any`.
 * Opaque types provide type safety without exposing the underlying representation.
 */
opaque type Token[+Name <: ValidName, +Ctx <: LexerCtx, +Value] = Any

/**
 * Represents a specific type of token definition that denotes an ignored token during the lexing process.
 *
 * Ignored tokens typically refer to tokens that are matched and processed by the lexer but are
 * excluded from the final parsed token stream. Examples of ignored tokens include whitespace,
 * comments, or any other tokens that are syntactically meaningful but do not contribute to
 * the structured representation of the input source.
 *
 * This opaque type is parameterized by a context type `Ctx`, which must be a subtype of `LexerCtx`.
 * The `LexerCtx` trait serves as a base for maintaining global lexing state, such as the current
 * position, the last matched token, and the remaining input.
 *
 * The `ValidName` and `Nothing` type parameters are placeholder constraints inherited from
 * `Token`, but `IgnoredToken` does not provide its own additional constraints
 * or behavior beyond being excluded from normal processing.
 *
 * The use of an opaque type ensures safe and restricted use within the scope of the lexer, as
 * this type cannot be directly manipulated outside the context of its definition.
 */
opaque type IgnoredToken[+Ctx <: LexerCtx] <: Token[ValidName, Ctx, Nothing] = Any
