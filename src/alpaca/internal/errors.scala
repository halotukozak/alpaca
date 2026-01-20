package alpaca
package internal

/**
 * Error message for methods that should only be called during parser definition.
 *
 * This constant is used in error messages to indicate that a method was called
 * outside of its intended context (the parser definition).
 */
final val ParserOnly = "Should never be called outside the parser definition"

/**
 * Error message for methods that should only be called during rule definition.
 *
 * This constant is used in error messages to indicate that a method was called
 * outside of its intended context (the rule definition).
 */
final val RuleOnly = "Should never be called outside the rule definition"

/**
 * Error message for methods that should only be called during conflict resolution.
 *
 * This constant is used in error messages to indicate that a method was called
 * outside of its intended context (the conflict resolution definition).
 */
final val ConflictResolutionOnly = "Should never be called outside the conflict resolution definition"
