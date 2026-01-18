package alpaca
package internal

import scala.util.control.NoStackTrace


abstract class AlpacaException(message: Shown) extends Exception(message) with NoStackTrace