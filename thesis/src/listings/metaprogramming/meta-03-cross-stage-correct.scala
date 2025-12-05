def safeQuote(using Quotes): Expr[Int] = {
  val localVar = 42
  Expr(localVar)  // OK: wartość jest serializowana do cytatu
}
