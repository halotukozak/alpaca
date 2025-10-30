package alpaca.parser

/**
 * Error message for methods that should only be called within parser definitions.
 *
 * This compile-time error is used to prevent calling certain methods outside
 * of their intended context.
 */
final val ParserOnly = "Should never be called outside the parser definition"

/**
 * Error message for methods that should only be called within rule definitions.
 *
 * This compile-time error is used to prevent calling certain methods outside
 * of their intended context.
 */
final val RuleOnly = "Should never be called outside the rule definition"

/**
 * Error message for methods that should only be called within conflict resolution definitions.
 *
 * This compile-time error is used to prevent calling certain methods outside
 * of their intended context.
 */
final val ConflictResolutionOnly = "Should never be called outside the conflict resolution definition"
