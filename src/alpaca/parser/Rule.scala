package alpaca
package parser

import alpaca.lexer.context.Lexem

import scala.annotation.compileTimeOnly

// todo: can we make it opaque with conversion?
type Rule[+T] = PartialFunction[Tuple | Lexem[?, ?], T]

object Rule {
  extension [T](rule: Rule[T]) {
    inline def alwaysAfter(rules: Rule[Any]*): Rule[T] = ???
    inline def alwaysBefore(rules: Rule[Any]*): Rule[T] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def unapply(x: Any): Option[T] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def List: PartialFunction[Any, Option[List[T]]] = ???

    @compileTimeOnly("Should never be called outside the parser definition")
    inline def Option: PartialFunction[Any, Option[T]] = ???
  }
}
