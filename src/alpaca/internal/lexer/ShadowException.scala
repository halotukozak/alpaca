package alpaca
package internal
package lexer

final class ShadowException private (message: Shown) extends AlpacaException(message):
  def this(first: String, second: String)(using DebugSettings) = this(show"Pattern $first is shadowed by $second")
