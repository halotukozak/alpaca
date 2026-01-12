inline def square(x: Int): Int = ${ squareImpl('x) }

def squareImpl(x: Expr[Int])(using Quotes): Expr[Int] = '{
  val squared = $x * $x
  squared
}

// usage: square(3) → will be expanded to: val squared = 3 * 3; squared
