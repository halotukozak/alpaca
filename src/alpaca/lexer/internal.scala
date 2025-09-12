package alpaca.lexer

import alpaca.core.raiseShouldNeverBeCalled

import scala.quoted.*
import scala.reflect.NameTransformer

// this util has bugs for sure https://github.com/halotukozak/alpaca/issues/51
private[lexer] final class CompileNameAndPattern[Q <: Quotes](using val quotes: Q) {
  import quotes.reflect.*

  def apply[T: Type](pattern: Tree): List[(name: Expr[ValidName], pattern: Expr[String])] = {
    // todo: @tailrec
    def nameLoop(pattern: Tree): List[String] = pattern match
      case Bind(_, Literal(StringConstant(str))) => str :: Nil
      case Bind(_, Alternatives(alternatives)) => alternatives.flatMap(nameLoop)
      case Literal(StringConstant(str)) => str :: Nil
      case Alternatives(alternatives) =>
        alternatives.flatMap(nameLoop).foldLeft("")(_ + "_or_" + _) :: Nil
      case x => raiseShouldNeverBeCalled(x.show)

    // todo: @tailrec
    def patternLoop(pattern: Tree): List[String] = pattern match
      case Bind(_, Literal(StringConstant(str))) => str :: Nil
      case Bind(_, Alternatives(alternatives)) => alternatives.flatMap(patternLoop)
      case Literal(StringConstant(str)) => str :: Nil
      case Alternatives(alternatives) =>
        alternatives.flatMap(patternLoop).foldLeft("")(_ + "|" + _) :: Nil
      case x => raiseShouldNeverBeCalled(x.show)

    def toResult(name: String, pattern: String) = (Expr(NameTransformer.decode(name)).asExprOf[ValidName], Expr(pattern))

    TypeRepr.of[T] match
      case ConstantType(StringConstant(str)) =>
        toResult(
          str,
          // it's wrong for sure
          patternLoop(pattern).headOption.getOrElse(str),
        ) :: Nil
      case _ => nameLoop(pattern).zip(patternLoop(pattern)).map(toResult)
  }
}
