package alpaca
package core

import scala.quoted.*

/** A type class for creating empty instances of types.
  *
  * This trait provides a way to create default instances of Product types (case classes)
  * by using their default parameter values. It extends Function0 to act as a factory.
  *
  * @tparam T the type to create empty instances of
  */
trait Empty[T] extends (() => T)

/** Companion object providing automatic derivation for the Empty type class. */
object Empty {

  /** Automatically derives an Empty instance for any Product type with default parameters.
    *
    * This macro-based derivation uses the default values of constructor parameters
    * to create a factory for the type.
    *
    * @tparam T the Product type to derive Empty for
    * @return an Empty instance that creates default instances
    */
  // either way it must be inlined for generic classes
  inline given derived[T <: Product]: Empty[T] = ${ derivedImpl[T] }

  private def derivedImpl[T <: Product: Type](using quotes: Quotes): Expr[Empty[T]] = {
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
