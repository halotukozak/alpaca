class SimpleLexer {
  val tokens: Map[String, Token[?, ?, ?]] = Map(
    "NUMBER" -> ...,
    "PLUS" -> ...
  )
  def apply(name: String): Token[?, ?, ?] = tokens(name)
}
