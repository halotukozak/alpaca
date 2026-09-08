package halotukozak
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
//todo: better name
infix class withDefault[T, Q]

trait withDefaultLowImplicitPriority:

  /**
   * Ignore default - use the provided type when explicitly specified.
   *
   * @tparam Provided the type that was explicitly provided
   * @tparam Default the default type (ignored)
   */
  inline given useProvided[Provided, Default]: (Provided withDefault Default) = new (Provided withDefault Default)

object withDefault extends withDefaultLowImplicitPriority:

  /**
   * Infer type argument to default when no type is explicitly provided.
   *
   * @tparam Default the default type to use
   */
  inline given useDefault[Default]: (Default withDefault Default) = new (Default withDefault Default)
