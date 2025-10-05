package alpaca.lexer.context
package default

final case class EmptyGlobalCtx(
  var text: CharSequence = "",
) extends GlobalCtx

final case class DefaultGlobalCtx(
  var text: CharSequence = "",
  var position: Int = 1,
  var line: Int = 1,
) extends GlobalCtx
    with PositionTracking
    with LineTracking
