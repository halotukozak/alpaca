package alpaca.core

/** A type-level marker used to provide default type parameters.
  *
  * This class is used internally for type inference to allow optional type parameters
  * with defaults in the lexer and parser APIs.
  *
  * @tparam T the provided type
  * @tparam Q the default type
  */
//todo: better name
infix private[alpaca] class WithDefault[T, Q]

trait WithDefaultLowImplicitPriority {

  /** Ignore default - use the provided type when explicitly specified.
    *
    * @tparam Provided the type that was explicitly provided
    * @tparam Default the default type (ignored)
    */
  given useProvided[Provided, Default]: (Provided WithDefault Default) = new (Provided WithDefault Default)
}

object WithDefault extends WithDefaultLowImplicitPriority {

  /** Infer type argument to default when no type is explicitly provided.
    *
    * @tparam Default the default type to use
    */
  given useDefault[Default]: (Default WithDefault Default) = new (Default WithDefault Default)
}
