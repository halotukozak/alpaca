package alpaca.core

//todo: better name
infix class :=[T, Q]

trait Default_:= {

  /** Ignore default */
  given useProvided[Provided, Default]: (Provided := Default) = new (Provided := Default)
}
object := extends Default_:= {

  /** Infer type argument to default */
  given useDefault[Default]: (Default := Default) = new (Default := Default)
}
