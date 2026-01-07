class MyLexer extends Tokenization[DefaultGlobalCtx]:
  val NUMBER = DefinedToken[...]
  val PLUS = DefinedToken[...]
  protected def compiled: Regex = "(?<token0>[0-9]+)|(?<token1>\\+)".r
  // ...
