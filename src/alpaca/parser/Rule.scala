package alpaca
package parser

import alpaca.lexer.context.Lexem

import scala.annotation.compileTimeOnly

/** Type alias for parser rules.
  *
  * A rule is a partial function that matches tuples (representing sequences
  * of symbols) or lexems and produces a result. Rules are used to define
  * grammar productions and their semantic actions.
  *
  * @tparam T the result type of the rule
  */
// todo: can we make it opaque with conversion?
type Rule[+T] = PartialFunction[Tuple | Lexem[?, ?], T]

/** Companion object providing extension methods for rules. */
object Rule {
  
  /** Extension methods for Rule instances. */
  extension [T](rule: Rule[T]) {
    
    /** Specifies that this rule should always come after the given rules.
      *
      * This is a compile-time only method for expressing rule precedence.
      *
      * @param rules the rules that should come before this rule
      * @return the modified rule
      */
    inline def alwaysAfter(rules: Rule[Any]*): Rule[T] = ???
    
    /** Specifies that this rule should always come before the given rules.
      *
      * This is a compile-time only method for expressing rule precedence.
      *
      * @param rules the rules that should come after this rule
      * @return the modified rule
      */
    inline def alwaysBefore(rules: Rule[Any]*): Rule[T] = ???

    /** Pattern matching extractor for use in other rules.
      *
      * This is compile-time only and should only be used inside parser definitions.
      *
      * @param x the value to match
      * @return Some(result) if the rule matches, None otherwise
      */
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def unapply(x: Any): Option[T] = ???

    /** Creates a repeating rule that matches zero or more occurrences.
      *
      * This is compile-time only and should only be used inside parser definitions.
      *
      * @return a partial function that produces a list of results
      */
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def List: PartialFunction[Any, Option[List[T]]] = ???

    /** Creates an optional rule that matches zero or one occurrence.
      *
      * This is compile-time only and should only be used inside parser definitions.
      *
      * @return a partial function that produces an optional result
      */
    @compileTimeOnly("Should never be called outside the parser definition")
    inline def Option: PartialFunction[Any, Option[T]] = ???
  }
}
