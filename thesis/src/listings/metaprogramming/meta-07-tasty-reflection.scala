inline def inspectFields[T]: List[String] = ${ inspectFieldsImpl[T] }

def inspectFieldsImpl[T: Type](using Quotes): Expr[List[String]] = {
  import quotes.reflect.*

  val tpe = TypeRepr.of[T]
  val fields = tpe.typeSymbol.declaredFields.map(_.name)

  Expr(fields)
}

// Przykład użycia:
case class Person(name: String, age: Int, city: String)
inspectFields[Person]  // → List("name", "age", "city")
