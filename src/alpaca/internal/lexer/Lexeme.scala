package alpaca
package internal
package lexer

import scala.NamedTuple.AnyNamedTuple

/**
 * A lexeme represents a token that has been matched and extracted from the input.
 *
 * A lexeme contains the token's name and the value that was extracted from
 * the matched text. This is the output of the tokenization process.
 *
 * Fields exposed via `selectDynamic` are kept as two parallel arrays plus the
 * matched `text` rather than a `Map[String, Any]`, so every token match only
 * allocates one small `Array[Any]` for the values (field names are cached
 * per-context-class). Lookup is a linear scan, which is optimal for the
 * tiny field counts typical of `LexerCtx` subclasses.
 *
 * @tparam Name the token name type
 * @tparam Value the value type
 * @param name the token name
 * @param value the extracted value
 */
private[alpaca] final class Lexeme[+Name <: ValidName, +Value](
  val name: Name,
  val value: Value,
  private[alpaca] val fieldNames: Array[String],
  private[alpaca] val fieldValues: Array[Any],
  private[alpaca] val text: String,
) extends Selectable:
  type Fields <: AnyNamedTuple

  def selectDynamic(name: String): Any =
    if name == "text" then text
    else
      var i = 0
      while i < fieldNames.length do
        if fieldNames(i) == name then return fieldValues(i)
        i += 1
      throw new NoSuchElementException(name)

  /**
   * Map view of all fields (including `text`). Used only for equality, tests,
   * and debugging — the hot path does a direct linear scan above.
   */
  private[alpaca] def fieldsAsMap: Map[String, Any] =
    val b = Map.newBuilder[String, Any]
    b.sizeHint(fieldNames.length + 1)
    var i = 0
    while i < fieldNames.length do
      b += ((fieldNames(i), fieldValues(i)))
      i += 1
    b += (("text", text))
    b.result()

  override def equals(other: Any): Boolean = other match
    case that: Lexeme[?, ?] => name == that.name && value == that.value && fieldsAsMap == that.fieldsAsMap
    case _ => false

  override def hashCode(): Int =
    var h = name.##
    h = 31 * h + value.##
    h = 31 * h + fieldsAsMap.##
    h

  override def toString: String = s"Lexeme($name, $value, $fieldsAsMap)"

private[alpaca] object Lexeme:

  private val EmptyFieldNames: Array[String] = Array.empty
  private val EmptyFieldValues: Array[Any] = Array.empty

  /**
   * Cached field-name array per `LexerCtx` subclass — `productElementNames`
   * is class-level metadata and never changes per instance.
   */
  private val fieldNameCache = new java.util.concurrent.ConcurrentHashMap[Class[?], Array[String]]

  private[alpaca] def fieldNamesFor(ctx: Product): Array[String] =
    fieldNameCache.computeIfAbsent(ctx.getClass, _ => ctx.productElementNames.toArray)

  /** Primary runtime constructor: zero-allocation field storage. */
  def apply[Name <: ValidName, Value](
    name: Name,
    value: Value,
    fieldNames: Array[String],
    fieldValues: Array[Any],
    text: String,
  ): Lexeme[Name, Value] = new Lexeme(name, value, fieldNames, fieldValues, text)

  /**
   * Convenience constructor for tests and ad-hoc use where callers provide a
   * plain `Map`. Splits the map into the internal arrays. `"text"` is
   * recognised as a reserved key for the matched input.
   */
  def apply[Name <: ValidName, Value](
    name: Name,
    value: Value,
    fields: Map[String, Any],
  ): Lexeme[Name, Value] =
    val text = fields.getOrElse("text", "").asInstanceOf[String]
    val rest = fields - "text"
    val names = rest.keysIterator.toArray
    val values = names.map(rest(_).asInstanceOf[Any])
    new Lexeme(name, value, names, values, text)

  /**
   * A special end-of-file lexeme used to signal the end of input.
   *
   * This is used internally by the parser to detect when all input has been consumed.
   */
  val EOF: Lexeme["$", String] = Lexeme("$", "", EmptyFieldNames, EmptyFieldValues, "")
