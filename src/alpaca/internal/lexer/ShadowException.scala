package alpaca
package internal
package lexer

import scala.annotation.constructorOnly

final class ShadowException(first: String, second: String)(using @constructorOnly log: Log)
  extends AlpacaException(show"Pattern $first is shadowed by $second")
