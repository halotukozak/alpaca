package alpaca.core

extension [A](self: A) {
  inline def tap[U](inline f: A => U): A = {
    f(self)
    self
  }
  
  inline def |>[B](inline f: A => B): B = f(self)
}
