package alpaca
package internal

/**
 * Type alias for valid token names.
 *
 * Token names must be singleton strings (string literals) to enable
 * compile-time type safety.
 */

//todo: make it opaque with ban on underscore https://github.com/halotukozak/alpaca/issues/223
type ValidName = String & Singleton

object ValidName:
  def from[Name <: ValidName: Type](using quotes: Quotes)(using Log): ValidName =
    import quotes.reflect.*
    logger.trace(show"extracting ValidName from ${Type.of[Name]}")
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str
      case x => raiseShouldNeverBeCalled(x.show)

  /**
   * Validates a token name during macro expansion.
   *
   * Token names must not be an underscore (_) as that would be invalid.
   *
   * @param name the token name to validate
   */
  def check(name: String)(using quotes: Quotes)(using Log): Unit =
    import quotes.reflect.*
    name match
      case invalid @ "_" => report.errorAndAbort(show"Invalid token name: $invalid")
      case _ =>
