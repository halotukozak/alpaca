package alpaca

import alpaca.core.Showable.Shown

import java.io.FileWriter
import scala.quoted.*
import scala.util.{Try, Using}

def symbolInfo(
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

def typeReprInfo(
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

def treeInfo(using quotes: Quotes)(tree: quotes.reflect.Tree): String = {
  import quotes.reflect.*

  s"""
     |Structure ${Printer.TreeStructure.show(tree)}
     |ShortCode ${Printer.TreeShortCode.show(tree)}
     |""".stripMargin
}

extension (using quotes: Quotes)(tree: quotes.reflect.Tree)
  def error: tree.type = {
    quotes.reflect.report.errorAndAbort(tree.show)
    tree
  }
  def info: tree.type = {
    quotes.reflect.report.info(tree.show)
    tree
  }

extension (using quotes: Quotes)(msg: String)
  def error: Nothing = quotes.reflect.report.errorAndAbort(msg)
  def info: Unit = quotes.reflect.report.info(msg)

extension (using quotes: Quotes)(e: Any)
  def dbg: Nothing = quotes.reflect.report.errorAndAbort(e.toString)
  def soft: e.type =
    quotes.reflect.report.info(e.toString)
    e

inline def showAst(inline body: Any) = ${ showAstImpl('{ body }) }
private def showAstImpl(body: Expr[Any])(using quotes: Quotes): Expr[Unit] = {
  import quotes.reflect.*

  Printer.TreeShortCode.show(body.asTerm.underlyingArgument).dbg
}

inline def showRawAst(inline body: Any) = ${ showRawAstImpl('{ body }) }
private def showRawAstImpl(body: Expr[Any])(using quotes: Quotes): Expr[Unit] = {
  import quotes.reflect.*

  Printer.TreeStructure.show(body.asTerm.underlyingArgument).dbg
}

def writeToFile(path: String)(content: Shown): Unit =
  Using.resource(new FileWriter(path))(_.write(content))
