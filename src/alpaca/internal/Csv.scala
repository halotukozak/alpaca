package alpaca
package internal

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
//todo: make Tuple-based in the future for better type safety and performance
private[internal] final case class Csv(
  headers: Flow[Shown],
  rows: Flow[Flow[Shown]],
)

private[internal] object Csv:

  /**
   * Showable instance for Csv that formats it as a comma-separated value string.
   *
   * The output has headers on the first line followed by data rows,
   * with values separated by commas.
   */

  /// todo it should be printed to the code lazy
  given Showable[Csv] = Showable: csv =>
    val header = csv.headers.mkShow(",")
    val rows = csv.rows.map(_.mkShow(",")).mkShow("\n")
    show"$header\n$rows"

  extension [N <: Tuple, V <: Tuple](rows: Flow[NamedTuple[N, V]])
    /**
     * Converts a list of named tuples to CSV format.
     *
     * The tuple field names become the CSV headers, and the values
     * become the data rows. This is useful for exporting structured
     * data to CSV.
     *
     * @return a Csv representation of the named tuples
     */

    inline def toCsv(using Log): Csv =
      Csv(
        compiletime.constValueTuple[N].toShowableFlow,
        rows.map(_.toTuple.toShowableFlow),
      )

  extension [T <: Tuple](tuple: T)
    inline private def toShowableList(using Log) = compiletime
      .summonAll[Tuple.Map[T, Showable]]
      .zip(tuple)
      .toList
      .asInstanceOf[List[(Showable[Any], Any)]]
      .map(_.show(_))

    inline def toShowableFlow(using Log) = compiletime
      .summonAll[Tuple.Map[T, Showable]]
      .zip(tuple)
      .toList //todo: nod needed
      .asFlow
      .asInstanceOf[Flow[(Showable[Any], Any)]]
      .map(_.show(_))
      
