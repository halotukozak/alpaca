package alpaca.lexer

trait Ctx {
  var text: String
  var index: Int
  var lineno: Int
}

inline given ctx: Ctx = compiletime.summonInline
