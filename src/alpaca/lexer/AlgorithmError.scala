package alpaca.lexer

/** Exception thrown when an algorithmic error occurs during lexing or parsing.
  *
  * This exception is used to signal errors in the internal algorithms used by
  * the lexer and parser generators, such as invalid state transitions or
  * configuration errors.
  *
  * @param message the error message describing what went wrong
  */
final class AlgorithmError(message: String) extends Exception(message)
