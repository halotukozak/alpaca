package alpaca
package internal

/**
 * Type alias for valid token names.
 *
 * Token names must be singleton strings (string literals) to enable
 * compile-time type safety.
 */

//todo: make it opaque with ban on underscore
type ValidName = String & Singleton

object ValidName {

  def from[Name <: ValidName: Type](using quotes: Quotes)(using DebugSettings): ValidName =
    import quotes.reflect.*
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str
      case x => raiseShouldNeverBeCalled(x.show)

  /**
   * Validates a token name during macro expansion.
   *
   * Token names must not be an underscore (_) as that would be invalid.
   *
   * @param name the token name to validate
   * @param quotes the Quotes instance
   */
  def check(name: String)(using quotes: Quotes): Unit =
    import quotes.reflect.*
    name match
      case invalid @ "_" => report.errorAndAbort(s"Invalid token name: $invalid")
      case _ =>
}
