inline def showType[T](x: T): String = ${ showTypeImpl('x) }

def showTypeImpl[T: Type](x: Expr[T])(using Quotes): Expr[String] = 
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  Expr(tpe.show)

// usage: showType(42)        // "scala.Int"
// usage: showType("hello")   // "java.lang.String"
