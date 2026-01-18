package alpaca
package internal

import scala.util.control.NoStackTrace

import alpaca.internal.Showable.given_Conversion_String_Shown

abstract class AlpacaException(message: Shown) extends Exception(message) with NoStackTrace

object AlpacaTimeoutException extends AlpacaException("Alpaca compilation timeout")
