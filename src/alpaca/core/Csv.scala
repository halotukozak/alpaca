package alpaca.core

import alpaca.core.Showable.{mkShow, mkShowTuple, Shown}

import scala.NamedTuple.NamedTuple

/**
 * Represents data in CSV (Comma-Separated Values) format.
 *
 * This case class holds tabular data with headers and rows that can be
 * displayed as a CSV string. It is used internally for debug output.
 *
 * Note: Future improvements may include making this Tuple-based for
 * better type safety and performance.
 *
 * @param headers the column headers
 * @param rows the data rows, each containing values for each column
 */
private[alpaca] final case class Csv(
  headers: List[Shown],
  rows: List[List[Shown]],
)

private[alpaca] object Csv {
  /**
   * Showable instance for Csv that formats it as a comma-separated value string.
   *
   * The output has headers on the first line followed by data rows,
   * with values separated by commas.
   */
  given Showable[Csv] = csv =>
    val header = csv.headers.mkShow(",")
    val rows = csv.rows.map(_.mkShow(",")).mkShow("\n")
    show"$header\n$rows"

  extension [N <: Tuple, V <: Tuple](rows: List[NamedTuple[N, V]])
    /**
     * Converts a list of named tuples to CSV format.
     *
     * The tuple field names become the CSV headers, and the values
     * become the data rows. This is useful for exporting structured
     * data to CSV.
     *
     * @return a Csv representation of the named tuples
     */
    inline def toCsv: Csv = Csv(
      mkShowTuple[N].toList.asInstanceOf[List[Shown]],
      rows.map(
        _.zip(compiletime.summonAll[Tuple.Map[V, Showable]]).toList
          .map { case (value, showable: Showable[Any] @unchecked) => showable.show(value) },
      ),
    )
}
