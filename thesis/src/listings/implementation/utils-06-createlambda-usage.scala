val createLambda = CreateLambda()
val lambdaExpr: Expr[Int => Int] = createLambda[Int => Int] { case (methodSymbol, List(argTree)) =>
// Konstrukcja ciała funkcji na podstawie symbolu metody i argumentów  buildBody(argTree)
}
