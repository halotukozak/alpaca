package alpaca.lexer

extension (input: CharSequence)
  def from(pos: Int): CharSequence = input match
    case lfr: LazyReader => lfr.from(pos)
    case _ => input.subSequence(pos, input.length)

  def close(): Unit = input match
    case lfr: LazyReader => lfr.close()
    case _ => ()
