def safeQuote(using Quotes): Expr[Int] = 
  val localVar = 42
  Expr(localVar)  // OK: local variable is quoted
