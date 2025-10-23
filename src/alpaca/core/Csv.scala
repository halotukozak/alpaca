package alpaca.core

import alpaca.core.Showable.{mkShow, mkShowTuple, Shown}

import scala.NamedTuple.NamedTuple

//todo: internal, unsafe, make it Tuple based
private[alpaca] final case class Csv(
  headers: List[Shown],
  rows: List[List[Shown]],
)

private[alpaca] object Csv {
  given Showable[Csv] = csv =>
    val header = csv.headers.mkShow(",")
    val rows = csv.rows.map(_.mkShow(",")).mkShow("\n")
    show"$header\n$rows"

  extension [N <: Tuple, V <: Tuple](rows: List[NamedTuple[N, V]])
    inline def toCsv: Csv = Csv(
      mkShowTuple[N].toList.asInstanceOf[List[Shown]],
      rows.map(
        _.zip(compiletime.summonAll[Tuple.Map[V, Showable]]).toList
          .map { case (value, showable: Showable[Any] @unchecked) => showable.show(value) },
      ),
    )
}
