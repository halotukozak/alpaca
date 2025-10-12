package alpaca.core

//todo: better name
infix private[alpaca] class WithDefault[T, Q]

private[alpaca] trait WithDefaultLowImplicitPriority {

  /** Ignore default */
  given useProvided[Provided, Default]: (Provided WithDefault Default) = new (Provided WithDefault Default)
}
private[alpaca] object WithDefault extends WithDefaultLowImplicitPriority {

  /** Infer type argument to default */
  given useDefault[Default]: (Default WithDefault Default) = new (Default WithDefault Default)
}
