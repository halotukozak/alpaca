package halotukozak
package alpaca.internal

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}
import scala.collection.mutable

/**
 * A TreeMap that replaces symbol references in a tree.
 *
 * This class is used during macro expansion to transform trees by
 * replacing references to specific symbols with replacement terms.
 * This is useful for adapting code from one context to another.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
// $COVERAGE-OFF$
private[internal] final class ReplaceRefs[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  /**
   * Creates a TreeMap that replaces symbol references.
   *
   * Given a sequence of (symbol to find, term to replace with) pairs,
   * this creates a TreeMap that will substitute all references to the
   * find symbols with the corresponding replacement terms.
   *
   * @param queries pairs of (symbol to find, replacement term)
   * @return a TreeMap that performs the replacements
   */
  def apply(queries: (find: Symbol, replace: Term)*): TreeMap = new TreeMap:
    // skip NoSymbol
    private val filtered = queries.filterNot(_.find.isNoSymbol)

    override def transformTerm(tree: Term)(owner: Symbol): Term =
      filtered
        .collectFirst:
          case (find, replace) if find == tree.symbol =>
            replace
        .getOrElse:
          val term = tree match
            case block: Block => block.changeOwner(owner)
            case other => other
          super.transformTerm(term)(owner)

/**
 * A helper for creating lambda expressions during macro expansion.
 *
 * This class provides a way to construct function expressions programmatically
 * by specifying how to build the function body given the parameter symbols.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
private[internal] final class CreateLambda[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  /**
   * Creates a lambda expression with a given body construction function.
   *
   * @tparam F the function type to create
   * @param rhsFn a function that builds the body tree given the method symbol and argument trees
   * @return an expression of type F
   */
  def apply[F: Type](rhsFn: PartialFunction[(Symbol, List[Tree]), Tree]): Expr[F] =
    require(TypeRepr.of[F].isFunctionType, show"Expected a function type, but got: ${TypeRepr.of[F]}")

    val params :+ r = TypeRepr.of[F].typeArgs.runtimeChecked

    Lambda(
      Symbol.spliceOwner,
      MethodType(params.zipWithIndex.map((_, i) => show"$$arg$i"))(_ => params, _ => r),
      (sym, args) =>
        if !rhsFn.isDefinedAt((sym, args)) then raiseShouldNeverBeCalled[(Symbol, List[Tree])]((sym, args))
        rhsFn.apply((sym, args)),
    ).asExprOf[F]

/**
 * ToExpr instance for NamedTuple.
 *
 * Allows named tuples to be lifted to expressions during macro expansion
 * by converting them to regular tuples.
 */
private[internal] given [K <: Tuple, V <: Tuple: ToExpr] => ToExpr[NamedTuple[K, V]]:
  def apply(x: NamedTuple[K, V])(using Quotes): Expr[NamedTuple[K, V]] = Expr(x.toTuple)

/**
 * ToExpr instance for nullable types.
 *
 * Handles conversion of nullable values to expressions, properly
 * distinguishing between null and non-null values.
 */
private[internal] given [T: {ToExpr as toExpr}] => ToExpr[T | Null]:
  def apply(x: T | Null)(using Quotes): Expr[T | Null] = x match
    case null => '{ null }
    case value: T @unchecked => toExpr.apply(value)

/**
 * FromExpr instance for nullable types.
 *
 * Handles extraction of nullable values from expressions during
 * macro expansion.
 */
private[internal] given [T: FromExpr as fromExpr] => FromExpr[T | Null]:
  def unapply(x: Expr[T | Null])(using Quotes): Option[T | Null] = x match
    case '{ $_ : Null } => Some(null)
    case value: Expr[T] @unchecked => fromExpr.unapply(value)
// $COVERAGE-ON$

/**
 * A helper class for overriding symbols in macro expansion.
 *
 * This class provides a way to create overriding symbols in a specific
 * parent context, which is useful for generating code that properly
 * respects symbol ownership.
 *
 * @tparam Q the Quotes type
 * @param quotes the Quotes instance
 */
// $COVERAGE-OFF$
private[internal] final class WithOverridingSymbol[Q <: Quotes](using val quotes: Q):
  import quotes.reflect.*

  /**
   * Creates and uses an overriding symbol.
   *
   * @tparam T the result type
   * @param parent the parent symbol context
   * @param symbol a function to create the base symbol
   * @param body the code to execute with the overriding symbol
   * @return the result of executing body
   */
  def apply[T](parent: Symbol)(symbol: Symbol => Symbol)(body: Quotes ?=> Symbol => T): T =
    val baseSymbol = symbol(parent)
    val owner = baseSymbol.overridingSymbol(parent) match
      case owner if owner.isNoSymbol =>
        baseSymbol
      case owner =>
        owner

    body(using owner.asQuotes)(owner)
// $COVERAGE-ON$

/**
 * Type-level operator for adding named fields to a type.
 *
 * This infix type operator allows specifying that a type B has named fields
 * of type A, enabling compile-time tracking of field information.
 *
 * @tparam B the base type with a Fields member
 * @tparam A the named tuple type representing the fields
 */
infix private[alpaca] type withFields[B <: { type Fields <: AnyNamedTuple }, A <: AnyNamedTuple] = B { type Fields = A }

/**
 * Creates a refinement type from a sequence of labeled types.
 *
 * Builds a type with structural refinements for each label/type pair.
 * This is used for creating types with named members dynamically.
 *
 * @param refn sequence of label and type pairs
 * @return a refined TypeRepr
 */
// $COVERAGE-OFF$
private[internal] def refinementTpeFrom(using quotes: Quotes)(refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*
  refn.foldLeft(TypeRepr.of[Any]):
    case (acc, (label, tpe)) => Refinement(acc, label, tpe)

/**
 * Creates a NamedTuple type from a sequence of labeled types.
 *
 * Constructs a NamedTuple TypeRepr with the given labels and types.
 * This is used for dynamically creating named tuple types.
 *
 * @param refn sequence of label and type pairs
 * @return a NamedTuple TypeRepr
 */
private[internal] def fieldsTpeFrom(using quotes: Quotes)(refn: Seq[(label: String, tpe: quotes.reflect.TypeRepr)])
  : quotes.reflect.TypeRepr =
  import quotes.reflect.*

  TypeRepr
    .of[NamedTuple]
    .appliedTo(
      refn
        .foldLeft((TypeRepr.of[EmptyTuple], TypeRepr.of[EmptyTuple])):
          case ((labels, types), (label, tpe)) =>
            (
              TypeRepr.of[*:].appliedTo(List(ConstantType(StringConstant(label)), labels)),
              TypeRepr.of[*:].appliedTo(List(tpe, types)),
            )
        .toList,
    )

private[alpaca] def avoidTooLargeMethod[A: Type, To: Type, B <: mutable.Builder[A, To]: Type](
  builder: Expr[B],
  elements: Iterable[Expr[A]],
  empty: Expr[To],
)(using quotes: Quotes,
): Expr[To] =
  import quotes.reflect.*
  if elements.isEmpty then empty
  else
    ValDef
      .let(Symbol.spliceOwner, "builder", builder.asTerm): ref =>
        val builder = ref.asExprOf[B]
        val additions = elements
          .map: entry =>
            '{
              def local(): Unit = $builder += $entry
              local()
            }.asTerm
          .toList
        Block(additions, '{ $builder.result() }.asTerm)
      .asExprOf[To]

/**
 * Qualifies `name` by source file and line, for use as a grammar export's id (see
 * `ALPACA_GRAMMAR_EXPORT_DIR`): the same declaration name (e.g. a reused `val Lexer = lexer{...}`)
 * can recur across files, or even within one at different scopes, and would otherwise collide in
 * a shared export directory.
 */
private[alpaca] def exportId(name: String)(using quotes: Quotes): String =
  import quotes.reflect.*
  val sourceFileName = Position.ofMacroExpansion.sourceFile.path.split("[/\\\\]").last.stripSuffix(".scala")
  val line = Position.ofMacroExpansion.startLine + 1
  s"$sourceFileName.$name@L$line"
// $COVERAGE-ON$

extension [T](t: T)
  inline private[alpaca] def tap[U](inline f: T => U): T =
    f(t)
    t
