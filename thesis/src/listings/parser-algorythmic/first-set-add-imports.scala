@tailrec
private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match {
  case Production.NonEmpty(lhs, NonEmptyList(head: Terminal, tail)) =>
    firstSet.updated(lhs, firstSet(lhs) + head)

  case Production.NonEmpty(lhs, NonEmptyList(head: NonTerminal, tail)) =>
    val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

    val production = tail match
      case head :: next => Production.NonEmpty(lhs, NonEmptyList(head, next*))
      case Nil => Production.Empty(lhs)

    if firstSet(head).contains(Symbol.Empty)
    then addImports(newFirstSet, production)
    else newFirstSet

  case Production.Empty(lhs) =>
    firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)
}
