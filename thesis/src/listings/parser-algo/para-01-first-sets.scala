  @tailrec
  private def addImports(firstSet: FirstSet, production: Production): FirstSet = production match {
    case Production.NonEmpty(lhs, NEL(head: Terminal, tail)) =>
      firstSet.updated(lhs, firstSet(lhs) + head)

    case Production.NonEmpty(lhs, NEL(head: NonTerminal, tail)) =>
      val newFirstSet = firstSet.updated(lhs, firstSet(lhs) ++ (firstSet(head) - Symbol.Empty))

      val nextProduction = tail match
        case head :: next => Production.NonEmpty(lhs, NEL(head, next*))
        case Nil => Production.Empty(lhs)

      if firstSet(head).contains(Symbol.Empty)
      then addImports(newFirstSet, nextProduction)
      else newFirstSet

    case Production.Empty(lhs) =>
      firstSet.updated(lhs, firstSet(lhs) + Symbol.Empty)
  }
