package alpaca
package core

import scala.quoted.*

/**
 * Type alias for valid token names.
 *
 * Token names must be singleton strings (string literals) to enable
 * compile-time type safety.
 */
private[alpaca] type ValidName = String & Singleton

object ValidName {

  def typeToString[Name <: ValidName: Type](using quotes: Quotes): ValidName =
    import quotes.reflect.*
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str
      case x => raiseShouldNeverBeCalled(x.show)
}
