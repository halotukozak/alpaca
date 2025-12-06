def unsafeQuote(using Quotes): Expr[Int] = {
  val localVar = 42
  '{ localVar }  // BŁĄD: localVar nie istnieje w fazie wykonania!
}
