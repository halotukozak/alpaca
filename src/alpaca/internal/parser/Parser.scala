package alpaca
package internal
package parser

import alpaca.internal.*
import alpaca.internal.lexer.Lexeme
import alpaca.internal.parser.*

import scala.NamedTuple.NamedTuple
import scala.annotation.{compileTimeOnly, tailrec}
import scala.collection.mutable

/**
 * A trait that provides compile-time access to named productions for use in conflict resolution definitions.
 *
 * It is typically used when specifying conflict resolutions, enabling you to refer to productions
 * in a type-safe and compile-time-checked manner.
 *
 * @note This is a compile-time only feature and should be used within parser definitions.
 */
transparent private[alpaca] trait ProductionSelector extends Selectable:
  def selectDynamic(name: String): Any

/**
 * Base class for parsers.
 *
 * Users should extend this class and define their grammar rules as `Rule` instances.
 * The parser uses an LR parsing algorithm with automatic parse table generation.
 *
 * @tparam Ctx the global context type, defaults to EmptyGlobalCtx
 */
abstract class Parser[Ctx <: ParserCtx](
  using Ctx withDefault ParserCtx.Empty,
)(using
  empty: Empty[Ctx],
  tables: Tables[Ctx],
):

  /**
   * The root rule of the grammar.
   *
   * This is the starting point for parsing.
   */
  val root: Rule[?]

  /**
   * The set of conflict resolution rules for this parser.
   *
   * Override this to resolve shift/reduce or reduce/reduce conflicts
   * by specifying precedence relationships between productions and tokens.
   */
  val resolutions: Set[ConflictResolution] = Set.empty

  /**
   * Provides compile-time access to named productions for use in conflict resolution definitions.
   *
   * This method allows you to reference productions by their names as defined in your parser.
   * It is typically used when specifying conflict resolutions, enabling you to refer to productions
   * in a type-safe and compile-time-checked manner.
   *
   * Example usage:
   * {{{
   *   override val resolutions = Set(
   *     production.plus.after(production.times)
   *   )
   * }}}
   *
   * @note This is a compile-time only feature and should be used within parser definitions.
   */
  @compileTimeOnly(ConflictResolutionOnly)
  transparent inline protected def production: ProductionSelector = ${ productionImpl }

  /**
   * Provides access to the parser context within rule definitions.
   *
   * This is compile-time only and can only be used inside parser rule definitions.
   */
  @compileTimeOnly(RuleOnly)
  inline protected final def ctx: Ctx = dummy

  /**
   * Parses a list of lexemes using the defined grammar.
   *
   * This method builds the parse table at compile time and uses it to
   * parse the input lexemes using an LR parsing algorithm.
   *
   * @tparam R the result type
   * @param lexemes the list of lexemes to parse
   * @return a tuple of (context, result), where result may be null on parse failure
   */
  private[alpaca] def unsafeParse[R](lexemes: List[Lexeme[?, ?]]): (ctx: Ctx, result: R | Null) =
    enum Node:
      case Result(value: Any)
      case Token(lexeme: Lexeme[?, ?])

      def get: Any = this match
        case Node.Result(value) => value
        case Node.Token(lexeme) => lexeme

    val ctx = empty()

    val stateStack = mutable.ArrayDeque.empty[Int]
    val nodeStack = mutable.ArrayDeque.empty[Node]
    stateStack += 0
    nodeStack += Node.Result(null)

    @tailrec def loop(remaining: List[Lexeme[?, ?]]): Node =
      val current = if remaining.isEmpty then Lexeme.EOF else remaining.head
      val nextSymbol = Terminal(current.name)
      tables.parseTable(stateStack.last, nextSymbol) match
        case ParseAction.Shift(gotoState) =>
          stateStack += gotoState
          nodeStack += Node.Token(current)
          loop(if remaining.isEmpty then Nil else remaining.tail)

        case ParseAction.Reduction(prod @ Production.NonEmpty(lhs, rhs, name)) =>
          val n = rhs.size
          val newStateIdx = stateStack(stateStack.size - 1 - n)

          if lhs == Symbol.Start && newStateIdx == 0 then nodeStack.last
          else
            val top = nodeStack.size - 1
            val children = Array.better.tabulate(n)(i => nodeStack(top - i).get)
            stateStack.dropRightInPlace(n)
            nodeStack.dropRightInPlace(n)

            val ParseAction.Shift(gotoState) = tables.parseTable(newStateIdx, lhs).runtimeChecked
            val result = tables.actionTable(prod)(ctx, RevertedArray(children))
            stateStack += gotoState
            nodeStack += Node.Result(result)
            loop(remaining)

        case ParseAction.Reduction(Production.Empty(Symbol.Start, name)) if stateStack.last == 0 =>
          nodeStack.last

        case ParseAction.Reduction(prod @ Production.Empty(lhs, name)) =>
          val ParseAction.Shift(gotoState) = tables.parseTable(stateStack.last, lhs).runtimeChecked
          val result = tables.actionTable(prod)(ctx, RevertedArray.empty)
          stateStack += gotoState
          nodeStack += Node.Result(result)
          loop(remaining)

    val result = loop(lexemes) match
      case Node.Result(value) => value.asInstanceOf[R]
      case Node.Token(lexeme) => null

    (ctx, result)

private val cachedProductions: mutable.Map[Type[? <: AnyKind], (Type[? <: AnyKind], Type[? <: AnyKind])] =
  mutable.Map.empty

// $COVERAGE-OFF$
def productionImpl(using quotes: Quotes): Expr[ProductionSelector] = withLog:
  import quotes.reflect.*

  val parserSymbol = Symbol.spliceOwner.owner.owner
  val parserTpe = parserSymbol.typeRef

  logger.trace(show"Generating production selector for $parserSymbol")

  cachedProductions
    .getOrElseUpdate(
      parserTpe.asType, {
        val rules = parserTpe.typeSymbol.declarations.iterator.collect:
          case decl if decl.typeRef <:< TypeRepr.of[Rule[?]] => decl.tree

        val extractName: PartialFunction[Expr[Rule[?]], Seq[String]] =
          case '{ rule(${ Varargs(cases) }*) } =>
            cases.flatMap:
              case '{ ($name: ValidName).apply($_ : ProductionDefinition[?]) } => name.value
              case _ => None

        val fields = rules
          .flatMap:
            case ValDef(name, _, Some(rhs)) =>
              logger.trace(show"Extracting production names from rule $name")
              extractName(rhs.asExprOf[Rule[?]])
            case DefDef(name, _, _, Some(rhs)) =>
              logger.trace(show"Extracting production names from rule $name")
              extractName(rhs.asExprOf[Rule[?]]) // todo: or error? https://github.com/halotukozak/alpaca/issues/230
            case _ =>
              report.error("Define resolutions as the last field of the parser.")
              Nil
          .map(name => (name, TypeRepr.of[Production]))
          .toList

        (refinementTpeFrom(fields).asType, fieldsTpeFrom(fields).asType)
      },
    )
    .runtimeChecked match
    case ('[refinement], '[fields]) =>
      '{ DummyProductionSelector.asInstanceOf[ProductionSelector { type Fields = fields } & refinement] }

private object DummyProductionSelector extends ProductionSelector:
  override def selectDynamic(name: String): Any = dummy
// $COVERAGE-ON$
