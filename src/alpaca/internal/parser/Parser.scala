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
 * This trait is used to provide compile-time access to named productions for use in conflict resolution definitions.
 * It is typically used when specifying conflict resolutions, enabling you to refer to productions
 * in a type-safe and compile-time-checked manner.
 *
 * @note This is a compile-time only feature and should be used within parser definitions.
 */
transparent trait ProductionSelector extends Selectable:
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
   * @param lexemes   the list of lexemes to parse
   * @return a tuple of (context, result), where result may be null on parse failure
   */
  private[alpaca] def unsafeParse[R](lexems: List[Lexeme[?, ?]]): (ctx: Ctx, result: R | Null) = supervisedWithLog:
    type Node = R | Lexeme[?, ?] | Null
    val ctx = empty()

    @tailrec def loop(lexems: List[Lexeme[?, ?]], stack: List[(index: Int, node: Node)]): R | Null =
      val nextSymbol = Terminal(lexems.head.name)
      tables.parseTable(stack.head.index, nextSymbol).runtimeChecked match
        case ParseAction.Shift(gotoState) =>
          loop(lexems.tail, (gotoState, lexems.head) :: stack)

        case ParseAction.Reduction(prod @ Production.NonEmpty(lhs, rhs, name)) =>
          val newStack = stack.drop(rhs.size)
          val newState = newStack.head

          if lhs == Symbol.Start && newState.index == 0 then stack.head.node.asInstanceOf[R | Null]
          else
            val ParseAction.Shift(gotoState) = tables.parseTable(newState.index, lhs).runtimeChecked
            val children = stack.take(rhs.size).map(_.node).reverse
            loop(
              lexems,
              (
                gotoState,
                tables.actionTable(prod)(ctx, children).asInstanceOf[Node],
              ) :: newStack,
            )

        case ParseAction.Reduction(Production.Empty(Symbol.Start, name)) if stack.head.index == 0 =>
          stack.head.node.asInstanceOf[R | Null]

        case ParseAction.Reduction(prod @ Production.Empty(lhs, name)) =>
          val ParseAction.Shift(gotoState) = tables.parseTable(stack.head.index, lhs).runtimeChecked
          loop(
            lexems,
            (gotoState, tables.actionTable(prod)(ctx, Nil).asInstanceOf[Node]) :: stack,
          )

    (ctx, loop(lexems :+ Lexeme.EOF, (0, null) :: Nil))

private val cachedProductions: mutable.Map[Type[? <: AnyKind], (Type[? <: AnyKind], Type[? <: AnyKind])] =
  mutable.Map.empty

def productionImpl(using quotes: Quotes): Expr[ProductionSelector] = supervisedWithLog:
  import quotes.reflect.*
  val parserSymbol = Symbol.spliceOwner.owner.owner
  val parserTpe = parserSymbol.typeRef

  Log.trace(show"Generating production selector for $parserSymbol")

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
              Log.trace(show"Extracting production names from rule $name")
              extractName(rhs.asExprOf[Rule[?]])
            case DefDef(name, _, _, Some(rhs)) =>
              Log.trace(show"Extracting production names from rule $name")
              extractName(rhs.asExprOf[Rule[?]]) // todo: or error?
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
