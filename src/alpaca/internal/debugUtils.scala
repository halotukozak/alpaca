package alpaca
package internal

import scala.util.Try
import Showable.given

/**
 * Generates a detailed string representation of a symbol during macro expansion.
 *
 * This function produces comprehensive information about a symbol including
 * its owner, flags, names, position, documentation, and structure. It is
 * useful for debugging macro code.
 *
 * @param quotes the Quotes instance
 * @param symbol the symbol to inspect
 * @return a multi-line string with detailed symbol information
 */
private[internal] def symbolInfo(
  using quotes: Quotes,
)(
  symbol: quotes.reflect.Symbol,
)(using quotes.reflect.Printer[quotes.reflect.TypeRepr],
): String =
  s"""
     |$symbol
     |maybeOwner: ${symbol.maybeOwner}
     |flags: ${symbol.flags.show}
     |privateWithin: ${symbol.privateWithin.map(_.show)}
     |protectedWithin: ${symbol.protectedWithin.map(_.show)}
     |name: ${symbol.name}
     |fullName: ${symbol.fullName}
     |pos: ${symbol.pos}
     |docstring: ${symbol.docstring}
     |tree: ${Try(symbol.tree.show).getOrElse("no tree")}
     |annotations: ${symbol.annotations.map(_.show)}
     |isDefinedInCurrentRun: ${symbol.isDefinedInCurrentRun}
     |isLocalDummy: ${symbol.isLocalDummy}
     |isRefinementClass: ${symbol.isRefinementClass}
     |isAliasType: ${symbol.isAliasType}
     |isAnonymousClass: ${symbol.isAnonymousClass}
     |isAnonymousFunction: ${symbol.isAnonymousFunction}
     |isAbstractType: ${symbol.isAbstractType}
     |isClassConstructor: ${symbol.isClassConstructor}
     |isSuperAccessor: ${symbol.isSuperAccessor}
     |isType: ${symbol.isType}
     |isTerm: ${symbol.isTerm}
     |isPackageDef: ${symbol.isPackageDef}
     |isClassDef: ${symbol.isClassDef}
     |isTypeDef: ${symbol.isTypeDef}
     |isValDef: ${symbol.isValDef}
     |isDefDef: ${symbol.isDefDef}
     |isBind: ${symbol.isBind}
     |isNoSymbol: ${symbol.isNoSymbol}
     |exists: ${symbol.exists}
     |declaredFields: ${symbol.declaredFields}
     |fieldMembers: ${symbol.fieldMembers}
     |declaredMethods: ${symbol.declaredMethods}
     |methodMembers: ${symbol.methodMembers}
     |declaredTypes: ${symbol.declaredTypes}
     |typeMembers: ${symbol.typeMembers}
     |declarations: ${symbol.declarations}
     |paramSymss: ${symbol.paramSymss}
     |allOverriddenSymbols: ${symbol.allOverriddenSymbols.toList}
     |primaryConstructor: ${symbol.primaryConstructor}
     |caseFields: ${symbol.caseFields}
     |isTypeParam: ${symbol.isTypeParam}
     |paramVariance: ${symbol.paramVariance.show}
     |signature: ${symbol.signature}
     |moduleClass: ${symbol.moduleClass}
     |companionClass: ${symbol.companionClass}
     |companionModule: ${symbol.companionModule}
     |children: ${symbol.children}
     |typeRef: ${Try(symbol.typeRef.show).getOrElse("no typeRef")}
     |termRef: ${Try(symbol.termRef.show).getOrElse("no termRef")}
     |""".stripMargin

/**
 * Generates a detailed string representation of a type during macro expansion.
 *
 * This function produces comprehensive information about a type including
 * its widened forms, symbols, base classes, and structural properties.
 * It is useful for debugging macro code.
 *
 * @param quotes the Quotes instance
 * @param tpe the type to inspect
 * @param printer implicit printer for type representations
 * @return a multi-line string with detailed type information
 */
private[internal] def typeReprInfo(
  using quotes: Quotes,
)(
  tpe: quotes.reflect.TypeRepr,
)(using quotes.reflect.Printer[quotes.reflect.TypeRepr],
): String =
  s"""
     |type: ${tpe.show}
     |widen: ${tpe.widen.show}
     |widenTermRefByName: ${tpe.widenTermRefByName.show}
     |widenByName: ${tpe.widenByName.show}
     |dealias: ${tpe.dealias.show}
     |dealiasKeepOpaques: ${tpe.dealiasKeepOpaques.show}
     |simplified: ${tpe.simplified.show}
     |classSymbol: ${tpe.classSymbol}
     |typeSymbol: ${tpe.typeSymbol}
     |termSymbol: ${tpe.termSymbol}
     |isSingleton: ${tpe.isSingleton}
     |baseClasses: ${tpe.baseClasses}
     |isFunctionType: ${tpe.isFunctionType}
     |isContextFunctionType: ${tpe.isContextFunctionType}
     |isErasedFunctionType: ${tpe.isErasedFunctionType}
     |isDependentFunctionType: ${tpe.isDependentFunctionType}
     |isTupleN: ${tpe.isTupleN}
     |typeArgs: ${tpe.typeArgs}
     |""".stripMargin

/**
 * Generates a string representation of a tree during macro expansion.
 *
 * This function shows both the structural representation and the short code
 * representation of a tree. It is useful for debugging macro code.
 *
 * @param quotes the Quotes instance
 * @param tree the tree to inspect
 * @return a multi-line string with tree structure and code
 */
private[internal] def treeInfo(using quotes: Quotes)(tree: quotes.reflect.Tree): String = {
  import quotes.reflect.*

  s"""
     |Structure ${Printer.TreeStructure.show(tree)}
     |ShortCode ${Printer.TreeShortCode.show(tree)}
     |""".stripMargin
}
private[internal] def positionInfo(using quotes: Quotes)(pos: quotes.reflect.Position): String =
  s"""
     |start: ${pos.start},
     |end: ${pos.end},
     |startLine: ${pos.startLine},
     |endLine: ${pos.endLine},
     |startColumn: ${pos.startColumn},
     |endColumn: ${pos.endColumn},
     |sourceFile: ${pos.sourceFile},
     |""".stripMargin

extension (using quotes: Quotes)(tree: quotes.reflect.Tree)
  /**
   * Prints the tree and aborts compilation at that point.
   *
   * This is useful for debugging macro code to see what tree
   * structure exists at a specific point.
   *
   * @param pos the source position of the debug call
   * @return the tree (never actually returns due to abort)
   */
  private[internal] def dbg(using pos: DebugPosition): tree.type = {
    import quotes.reflect.*
    report.errorAndAbort(show"${Printer.TreeShortCode.show(tree)} $pos")
    tree
  }

  /**
   * Prints the tree as an info message during compilation.
   *
   * This is useful for debugging macro code without aborting compilation.
   *
   * @param pos the source position of the debug call
   * @return the tree unchanged
   */
  private[internal] def info(using pos: DebugPosition): tree.type = {
    import quotes.reflect.*
    report.info(show"${Printer.TreeShortCode.show(tree)} $pos")
    tree
  }

extension (using quotes: Quotes)(expr: Expr[?])

  /**
   * Prints the expression and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression (never actually returns due to abort)
   */
  private[internal] def dbg(using pos: DebugPosition): expr.type =
    import quotes.reflect.*
    expr.asTerm.dbg
    expr

  /**
   * Prints the expression as an info message during compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression unchanged
   */
  private[internal] def soft(using pos: DebugPosition): expr.type =
    import quotes.reflect.*
    expr.asTerm.info
    expr

extension (using quotes: Quotes)(msg: String)
  /**
   * Prints a debug message and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @throws Nothing always aborts compilation
   */
  private[internal] def dbg(using pos: DebugPosition): Nothing =
    quotes.reflect.report.errorAndAbort(show"$msg $pos")

  /**
   * Prints a debug message as an info during compilation.
   *
   * @param pos the source position of the debug call
   */
  private[internal] def soft(using pos: DebugPosition): Unit =
    quotes.reflect.report.info(show"$msg $pos")

extension (using quotes: Quotes)(e: Any)
  /**
   * Prints any value's string representation and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @throws Nothing always aborts compilation
   */
  private[internal] def dbg(using pos: DebugPosition): Nothing =
    quotes.reflect.report.errorAndAbort(show"${e.toString} $pos")

  /**
   * Prints any value's string representation as an info message.
   *
   * @param pos the source position of the debug call
   * @return the value unchanged
   */
  private[internal] def soft(using pos: DebugPosition): e.type =
    quotes.reflect.report.info(show"${e.toString} $pos")
    e

/**
 * Shows the AST (Abstract Syntax Tree) of a code block and aborts compilation.
 *
 * This macro prints the tree structure of the given code in short form
 * and aborts compilation. Useful for understanding what the compiler sees.
 *
 * @param body the code to inspect
 * @param pos implicit source position
 */
inline private[internal] def showAst(inline body: Any)(using pos: DebugPosition) = ${ showAstImpl('{ body }, '{ pos }) }
private def showAstImpl(body: Expr[Any], pos: Expr[DebugPosition])(using quotes: Quotes): Expr[Unit] =
  import quotes.reflect.*
  Printer.TreeShortCode.show(body.asTerm.underlyingArgument).dbg(using pos.valueOrAbort)

/**
 * Shows the raw AST structure of a code block and aborts compilation.
 *
 * This macro prints the detailed tree structure of the given code
 * and aborts compilation. Useful for deep debugging of macro code.
 *
 * @param body the code to inspect
 * @param pos implicit source position
 */
inline private[internal] def showRawAst(inline body: Any)(using pos: DebugPosition) =
  ${ showRawAstImpl('{ body }, '{ pos }) }
private def showRawAstImpl(body: Expr[Any], pos: Expr[DebugPosition])(using quotes: Quotes): Expr[Unit] =
  import quotes.reflect.*
  Printer.TreeStructure.show(body.asTerm.underlyingArgument).dbg(using pos.valueOrAbort)
