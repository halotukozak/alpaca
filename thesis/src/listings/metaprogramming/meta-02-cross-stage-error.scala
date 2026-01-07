def unsafeQuote(using Quotes): Expr[Int] = 
  val localVar = 42
  '{ localVar }  // error: access to value localVar from wrong staging level
