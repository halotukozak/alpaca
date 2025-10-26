package alpaca

import alpaca.core.{show, DebugSettings, Showable}
import alpaca.core.Showable.Shown

import java.io.{File, FileWriter}
import scala.quoted.*
import scala.util.{Try, Using}

/**
 * Generates a detailed string representation of a symbol during macro expansion.
 *
 * This function produces comprehensive information about a symbol including
 * its owner, flags, names, position, documentation, and structure. It is
 * useful for debugging macro code.
 *
 * @param quotes the Quotes instance
 * @param symbol the symbol to inspect
 * @param printer implicit printer for type representations
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
private[alpaca] def treeInfo(using quotes: Quotes)(tree: quotes.reflect.Tree): String = {
  import quotes.reflect.*

  s"""
     |Structure ${Printer.TreeStructure.show(tree)}
     |ShortCode ${Printer.TreeShortCode.show(tree)}
     |""".stripMargin
}
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

/**
 * An opaque type representing a source code position for debug messages.
 *
 * This type wraps a line number and is used to annotate debug output
 * with the location where a debug call was made.
 */
opaque private[alpaca] type DebugPosition = Int

private[alpaca] object DebugPosition {

  /**
   * Implicit instance that captures the current source line number.
   *
   * When used in a debug call, this automatically provides the line number
   * where the call was made.
   */
  inline given here: DebugPosition = ${ hereImpl }

  private def hereImpl(using quotes: Quotes): Expr[DebugPosition] = {
    import quotes.reflect.*
    val pos = Position.ofMacroExpansion
    Expr(pos.startLine + 1)
  }

  given ToExpr[DebugPosition] with
    def apply(x: DebugPosition)(using quotes: Quotes): Expr[DebugPosition] =
      ToExpr.IntToExpr(x)

  given FromExpr[DebugPosition] with
    def unapply(x: Expr[DebugPosition])(using Quotes): Option[DebugPosition] =
      FromExpr.IntFromExpr.unapply(x)
}

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
  private[alpaca] def dbg(using pos: DebugPosition): tree.type = {
    quotes.reflect.report.errorAndAbort(show"$tree at line $pos")
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
  private[alpaca] def info(using pos: DebugPosition): tree.type = {
    quotes.reflect.report.info(show"$tree at line $pos")
    tree
  }

extension (using quotes: Quotes)(expr: Expr[?])

  /**
   * Prints the expression and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression (never actually returns due to abort)
   */
  private[alpaca] def dbg(using pos: DebugPosition): expr.type =
    import quotes.reflect.*
    expr.asTerm.dbg
    expr

  /**
   * Prints the expression as an info message during compilation.
   *
   * @param pos the source position of the debug call
   * @return the expression unchanged
   */
  private[alpaca] def soft(using pos: DebugPosition): expr.type =
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
  private[alpaca] def dbg(using pos: DebugPosition): Nothing =
    quotes.reflect.report.errorAndAbort(show"$msg at line $pos")

  /**
   * Prints a debug message as an info during compilation.
   *
   * @param pos the source position of the debug call
   */
  private[alpaca] def soft(using pos: DebugPosition): Unit = quotes.reflect.report.info(show"$msg at line $pos")

extension (using quotes: Quotes)(e: Any)
  /**
   * Prints any value's string representation and aborts compilation.
   *
   * @param pos the source position of the debug call
   * @throws Nothing always aborts compilation
   */
  private[alpaca] def dbg(using pos: DebugPosition): Nothing =
    quotes.reflect.report.errorAndAbort(show"${e.toString} at line $pos")

  /**
   * Prints any value's string representation as an info message.
   *
   * @param pos the source position of the debug call
   * @return the value unchanged
   */
  private[alpaca] def soft(using pos: DebugPosition): e.type =
    quotes.reflect.report.info(show"${e.toString} at line $pos")
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
inline private[alpaca] def showAst(inline body: Any)(using pos: DebugPosition) = ${ showAstImpl('{ body }, '{ pos }) }
private def showAstImpl(body: Expr[Any], pos: Expr[DebugPosition])(using quotes: Quotes): Expr[Unit] = {
  import quotes.reflect.*

  Printer.TreeShortCode.show(body.asTerm.underlyingArgument).dbg(using pos.valueOrAbort)
}

/**
 * Shows the raw AST structure of a code block and aborts compilation.
 *
 * This macro prints the detailed tree structure of the given code
 * and aborts compilation. Useful for deep debugging of macro code.
 *
 * @param body the code to inspect
 * @param pos implicit source position
 */
inline private[alpaca] def showRawAst(inline body: Any)(using pos: DebugPosition) = ${
  showRawAstImpl('{ body }, '{ pos })
}
private def showRawAstImpl(body: Expr[Any], pos: Expr[DebugPosition])(using quotes: Quotes): Expr[Unit] = {
  import quotes.reflect.*

  Printer.TreeStructure.show(body.asTerm.underlyingArgument).dbg(using pos.valueOrAbort)
}

/**
 * Writes debug content to a file if debug settings are enabled.
 *
 * This function conditionally writes the content to a file in the
 * debug directory only when debugging is enabled in the debug settings.
 * The directory structure is created if it doesn't exist.
 *
 * @param path the relative path within the debug directory
 * @param content the content to write
 * @param debugSettings the debug settings determining if/where to write
 */
private[alpaca] def debugToFile(path: String)(content: Shown)(using debugSettings: DebugSettings[?, ?]): Unit =
  if debugSettings.enabled then
    val file = new File(s"${debugSettings.directory}$path")
    file.getParentFile.mkdirs()
    Using.resource(new FileWriter(file))(_.write(content))
