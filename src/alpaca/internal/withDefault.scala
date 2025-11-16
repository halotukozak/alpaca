package alpaca
package internal

/**
 * A type-level marker used to provide default type parameters.
 *
 * This class is used internally for type inference to allow optional type parameters
 * with defaults in the lexer and parser APIs.
 *
 * @tparam T the provided type
 * @tparam Q the default type
 */
infix private[alpaca] class withDefault[T, Q]

private[alpaca] trait withDefaultLowImplicitPriority {

  /**
   * Ignore default - use the provided type when explicitly specified.
   *
   * @tparam Provided the type that was explicitly provided
   * @tparam Default the default type (ignored)
   */
  given useProvided[Provided, Default]: (Provided withDefault Default) = new (Provided withDefault Default)
}

private[alpaca] object withDefault extends withDefaultLowImplicitPriority {

  /**
   * Infer type argument to default when no type is explicitly provided.
   *
   * @tparam Default the default type to use
   */
  given useDefault[Default]: (Default withDefault Default) = new (Default withDefault Default)
}
