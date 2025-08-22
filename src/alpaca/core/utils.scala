package alpaca.core

private object NoValueMarker
private val NoValueMarkerFunc = (_: Any) => NoValueMarker

extension [T](x: T)
  infix def matchOption[R](pf: PartialFunction[T, R]): Option[R] =
    pf.applyOrElse[T, Any](x, NoValueMarkerFunc) match
      case NoValueMarker => None
      case rawValue => Some(rawValue.asInstanceOf[R])
