package alpaca
package internal

import scala.util.Try
import ox.*

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
private[alpaca] def symbolInfo(
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
private[alpaca] def typeReprInfo(
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
private[alpaca] def treeInfo(using quotes: Quotes)(tree: quotes.reflect.Tree): String =
  import quotes.reflect.*
  s"""
     |Structure ${Printer.TreeStructure.show(tree)}
     |ShortCode ${Printer.TreeShortCode.show(tree)}
     |""".stripMargin

private[alpaca] def positionInfo(using quotes: Quotes)(pos: quotes.reflect.Position): String =
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
  private[alpaca] def dbg(using pos: DebugPosition)(using Log): tree.type =
    quotes.reflect.report.errorAndAbort(show"$tree at $pos")
    tree

  /**
   * Prints the tree as an info message during compilation.
   *
   * This is useful for debugging macro code without aborting compilation.
   *
   * @param pos the source position of the debug call
   * @return the tree unchanged
   */
  private[alpaca] def info(using pos: DebugPosition)(using Log): tree.type =
    quotes.reflect.report.info(show"$tree at $pos")
    tree

extension (using quotes: Quotes)(expr: Expr[?])

  /**
   * Prints the expression and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression (never actually returns due to abort)
   */
  private[alpaca] def dbg(using pos: DebugPosition)(using Log): expr.type =
    import quotes.reflect.*
    expr.asTerm.dbg
    expr

  /**
   * Prints the expression as an info message during compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression unchanged
   */
  private[alpaca] def soft(using pos: DebugPosition)(using Log): expr.type =
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
  private[alpaca] def dbg(using pos: DebugPosition)(using Log): Nothing =
    quotes.reflect.report.errorAndAbort(show"$msg at $pos")

  /**
   * Prints a debug message as an info during compilation.
   *
   * @param pos the source position of the debug call
   */
  private[alpaca] def soft(using pos: DebugPosition)(using Log): Unit =
    quotes.reflect.report.info(show"$msg at $pos")

extension (using quotes: Quotes)(e: Any)
  /**
   * Prints any value's string representation and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @throws Nothing always aborts compilation
   */
  private[alpaca] def dbg(using pos: DebugPosition)(using Log): Nothing =
    quotes.reflect.report.errorAndAbort(show"${e.toString} at $pos")

  /**
   * Prints any value's string representation as an info message.
   *
   * @param pos the source position of the debug call
   * @return the value unchanged
   */
  private[alpaca] def soft(using pos: DebugPosition)(using Log): e.type =
    quotes.reflect.report.info(show"${e.toString} at $pos")
    e

/**
 * Shows the AST (Abstract Syntax Tree) of a code block and aborts compilation.
 *
 * This macro prints the tree structure of the given code in short form
 * and aborts compilation. Useful for understanding what the compiler sees.
 *
 * @param body the code to inspect
 */
inline private[alpaca] def showAst(inline body: Any) = ${ showAstImpl('{ body }) }

private def showAstImpl(body: Expr[Any])(using quotes: Quotes): Expr[Unit] = supervised:
  given Log = new Log
  import quotes.reflect.*
  Printer.TreeShortCode.show(body.asTerm.underlyingArgument).dbg

/**
 * Shows the raw AST structure of a code block and aborts compilation.
 *
 * This macro prints the detailed tree structure of the given code
 * and aborts compilation. Useful for deep debugging of macro code.
 *
 * @param body the code to inspect
 */
inline private[alpaca] def showRawAst(inline body: Any) = ${ showRawAstImpl('{ body }) }

private def showRawAstImpl(body: Expr[Any])(using quotes: Quotes): Expr[Unit] = supervised:
  given Log = new Log
  import quotes.reflect.*
  Printer.TreeStructure.show(body.asTerm.underlyingArgument).dbg
