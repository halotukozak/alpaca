inline def square(x: Int): Int = ${ squareImpl('x) }

def squareImpl(x: Expr[Int])(using Quotes): Expr[Int] = '{
  val squared = $x * $x
  squared
}

// Użycie: square(3) → rozwinie się do: val squared = 3 * 3; squared
