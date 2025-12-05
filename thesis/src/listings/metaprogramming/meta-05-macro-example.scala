inline def showType[T](x: T): String = ${ showTypeImpl('x) }

def showTypeImpl[T: Type](x: Expr[T])(using Quotes): Expr[String] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  Expr(tpe.show)
}

// Użycie:
showType(42)        // → "scala.Int"
showType("hello")   // → "java.lang.String"
