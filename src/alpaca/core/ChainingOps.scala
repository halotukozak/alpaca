package alpaca.core

/**
 * Extension methods for chaining operations on values.
 *
 * Provides utility methods for functional programming patterns.
 */
extension [A](self: A) {

  /**
   * Applies a side-effecting function to the value and returns the original value.
   *
   * Useful for debugging or performing side effects in a chain of operations.
   *
   * @param f the function to apply for its side effects
   * @return the original value
   */
  inline def tap[U](inline f: A => U): A = {
    f(self)
    self
  }

  /**
   * Applies a function to the value (forward pipe operator).
   *
   * Allows for more readable function composition by piping a value through a function.
   *
   * @param f the function to apply
   * @return the result of applying f to the value
   */
  inline def |>[B](inline f: A => B): B = f(self)
}
