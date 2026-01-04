def verifyNoConflicts(): Unit = {
  enum VisitState:
    case Unvisited, Visited, Processed

  enum Action:
    case Enter(node: ConflictKey, path: List[ConflictKey] = Nil)
    case Leave(node: ConflictKey)

  val visited = mutable.Map.empty[ConflictKey, VisitState].withDefaultValue(VisitState.Unvisited)

  @tailrec
  def loop(stack: List[Action]): Unit = stack match
    case Nil => // Done

    case Action.Leave(node) :: rest =>
      visited(node) = VisitState.Processed
      loop(rest)

    case Action.Enter(node, path) :: rest =>
      visited(node) match
        case VisitState.Processed => loop(rest)
        case VisitState.Visited => throw InconsistentConflictResolution()
        case VisitState.Unvisited =>
          visited(node) = VisitState.Visited
          val neighbors = table.getOrElse(node, Set.empty).map(Action.Enter(_, node :: path))
          loop(neighbors ::: List(Action.Leave(node)) ::: rest)

  for node <- table.keys do loop(Action.Enter(node) :: Nil)
}
