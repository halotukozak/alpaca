package alpaca.core

import scala.quoted.*

opaque type NonEmptyList[+A] <: List[A] = List[A]

object NonEmptyList {
  def apply[A](head: A, tail: A*): NonEmptyList[A] = head :: tail.toList

  def unsafe[A](list: List[A]): NonEmptyList[A] = list match
    case head :: tail => list
    case Nil => throw IllegalArgumentException("Empty list cannot be converted to NonEmptyList")

  def unapply[A](list: NonEmptyList[A]): Option[(A, NonEmptyList[A])] = list match
    case head :: tail => Some((head, tail))
    case _ => None

  given [A: {Type, ToExpr}]: ToExpr[NonEmptyList[A]] with
    def apply(x: NonEmptyList[A])(using Quotes): Expr[NonEmptyList[A]] =
      val list = x.map(Expr.apply)
      '{ NonEmptyList(${ list.head }, ${ Varargs(list.tail) }*) }
}
