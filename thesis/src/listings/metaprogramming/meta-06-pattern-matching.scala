inline def optimize(x: Int): Int = ${ optimizeImpl('x) }

def optimizeImpl(x: Expr[Int])(using Quotes): Expr[Int] = x match {
  case '{ 0 + $y }       => y  // 0 + y → y
  case '{ $y + 0 }       => y  // y + 0 → y
  case '{ 1 * $y }       => y  // 1 * y → y
  case '{ $y * 1 }       => y  // y * 1 → y
  case '{ 0 * $y }       => '{ 0 }  // 0 * y → 0
  case '{ $x + ($y + $z) } => '{ $x + $y + $z }  // reassocjacja
  case _ => x  // brak optymalizacji
}

// Przykład użycia:
optimize(0 + 5)     // → 5
optimize(3 * 1)     // → 3
optimize(0 * 100)   // → 0
