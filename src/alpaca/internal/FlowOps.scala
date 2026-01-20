package alpaca
package internal

export ox.flow.Flow

val threads = Runtime.getRuntime.availableProcessors

extension [C[X] <: Iterable[X], T](c: C[T]) inline def asFlow: Flow[T] = Flow.fromIterable(c)

extension [C[X] <: Iterator[X], T](c: C[T]) inline def asFlow: Flow[T] = Flow.fromIterator(c)

extension [A](flow: Flow[A])
  inline def head: A = flow.take(1).runLast()
  inline def headOption: Option[A] = flow.take(1).runLastOption()
  inline def collectFirst[B](f: PartialFunction[A, B]): Option[B] = flow.collect(f).headOption
  inline def filterNot(inline predicate: A => Boolean): Flow[A] = flow.filter(!predicate(_))
  inline def partition(inline predicate: A => Boolean): (Flow[A], Flow[A]) =
    flow.foldLeft((Flow.empty[A], Flow.empty[A])):
      case ((trueFlow, falseFlow), a) if predicate(a) => (trueFlow.append(a), falseFlow)
      case ((trueFlow, falseFlow), a) => (trueFlow, falseFlow.append(a))

  inline private def foldLeft[B](initial: B)(inline op: (B, A) => B): B = flow.scan(initial)(op).runLast()
  inline def runToMap[K, V]()(using A <:< (K, V)): Map[K, V] = flow.runToList().toMap
  inline def runToSet(): Set[A] = flow.runToList().toSet
  inline def reverse: Flow[A] = flow.foldLeft(Flow.empty[A]):
    case (acc, a) => acc.prepend(a)

  inline def find(inline predicate: A => Boolean): Option[A] = flow.filter(predicate).headOption
  inline def tail: Flow[A] = flow.drop(1)

  inline def unzip[K, V](using inline asPair: A => (K, V)): (Flow[K], Flow[V]) =
    flow.foldLeft((Flow.empty[K], Flow.empty[V])):
      case ((ks, vs), a) =>
        val (k, v) = asPair(a)
        (ks.append(k), vs.append(v))

  inline def unzip3[K, V, W](using inline asTriple: A => (K, V, W)): (Flow[K], Flow[V], Flow[W]) =
    flow.foldLeft((Flow.empty[K], Flow.empty[V], Flow.empty[W])):
      case ((ks, vs, ws), a) =>
        val (k, v, w) = asTriple(a)
        (ks.append(k), vs.append(v), ws.append(w))

  inline def prepend(e: A): Flow[A] = Flow.fromValues(e) ++ flow
  inline def append(e: A): Flow[A] = flow ++ Flow.fromValues(e)
  inline def tapFlow(f: Flow[A] => Unit): Flow[A] =
    f(flow)
    flow
  inline def indices: Flow[Long] = flow.zipWithIndex.map(_._2)

  // todo: make it more efficient
  inline def mkString(prefix: String, separator: String, suffix: String): String =
    flow.runToList().mkString(prefix, separator, suffix)

  inline def mkString(separator: String): String = mkString("", separator, "")

  inline def mkString: String = mkString("")
