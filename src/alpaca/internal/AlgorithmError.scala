package alpaca.internal

/**
 * Exception thrown when an algorithmic error occurs during lexing or parsing.
 *
 * This exception is used to signal errors in the internal algorithms used by
 * the lexer and parser generators. This indicates a library bug and should be
 * reported as a GitHub issue.
 *
 * @param message the error message describing what went wrong
 */
private[internal] final class AlgorithmError(message: String) extends Exception(message)
