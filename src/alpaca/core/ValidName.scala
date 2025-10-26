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

  /**
   * Given instance to extract the value of a ValidName at compile time.
   */
  given FromExpr[ValidName] with
    def unapply(x: Expr[ValidName])(using Quotes): Option[ValidName] =
      FromExpr.StringFromExpr[ValidName].unapply(x)

  given ToExpr[ValidName] with
    def apply(x: ValidName)(using Quotes): Expr[ValidName] =
      ToExpr.StringToExpr[ValidName].apply(x)

  def typeToString[Name <: ValidName: Type](using quotes: Quotes): ValidName =
    import quotes.reflect.*
    TypeRepr.of[Name] match
      case ConstantType(StringConstant(str)) => str
      case x => raiseShouldNeverBeCalled(x.show)
}
