package alpaca.parser.context
package default

/** An empty parser context with no state.
  *
  * This is the default context used by parsers when no custom context
  * is needed. Most simple parsers can use this.
  */
final case class EmptyGlobalCtx(
) extends GlobalCtx
