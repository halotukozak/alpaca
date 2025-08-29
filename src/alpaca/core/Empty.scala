package alpaca
package core

import scala.quoted.*

trait Empty[T] extends (() => T)

object Empty {
  inline def derived[T <: Product]: Empty[T] = ${ derivesImpl[T] }

  private def derivesImpl[T <: Product: Type](using quotes: Quotes): Expr[Empty[T]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    val constructor = tpe.classSymbol.get.primaryConstructor

    val defaultParameters = tpe.classSymbol.get.companionClass.methodMembers.collect {
      case m if m.name.startsWith("$lessinit$greater$default$") =>
        m.name.stripPrefix("$lessinit$greater$default$").toInt - 1 -> Ref(m)
    }.toMap

    val parameters = constructor.paramSymss.collect {
      case params if !params.exists(_.isTypeParam) =>
        params.zipWithIndex.map {
          case (param, idx) if param.flags.is(Flags.HasDefault) =>
            defaultParameters(idx)
          case (param, idx) =>
            report.errorAndAbort(
              s"Cannot derive Empty for ${Type.show[T]}: parameter ${param.name} does not have a default value",
            )
        }
    }

    val value =
      New(TypeTree.of[T])
        .select(constructor)
        .appliedToTypes(tpe.typeArgs)
        .appliedToArgss(parameters)
        .asExprOf[T]

    '{
      new Empty[T] {
        def apply(): T = $value
      }
    }
  }

}
