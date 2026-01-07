val createLambda = CreateLambda()
val lambdaExpr: Expr[Int => Int] = createLambda[Int => Int] { case (methodSymbol, List(argTree)) =>
// building function body based on method symbol and arguments
}
