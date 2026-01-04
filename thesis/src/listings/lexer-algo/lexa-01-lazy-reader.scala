  @tailrec
  private def ensure(pos: Int): Unit =
    if pos >= buffer.length then
      val charsRead = reader.read(chunk)
      if charsRead == -1 then
        throw new IndexOutOfBoundsException(s"Position $pos is out of bounds for LazyReader of size $size")
    else
      buffer.appendAll(chunk.iterator.take(charsRead))
      ensure(pos)
