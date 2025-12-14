def getHigherPrecedenceAction(first: ParseAction, second: ParseAction): Option[ParseAction] = {
  def winsOver(first: ParseAction, second: ParseAction): Option[ParseAction] = {
    @tailrec
    def loop(queue: List[ParseAction], visited: Set[ParseAction]): Option[ParseAction] = queue match
      case Nil => None
      case `second` :: _ => Some(first)
      case head :: tail =>
        val current = table.getOrElse(head, Set.empty)
        val neighbors = current.diff(visited)
        loop(tail ++ neighbors, visited + head)

    loop(List(first), Set())
  }

  winsOver(first, second) orElse winsOver(second, first)
}
