package example

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

var debug: Boolean = compiletime.uninitialized

val errors_and_warnings = collection.mutable.Map
  .empty[Int, collection.mutable.ListBuffer[(String, String)]]
  .withDefaultValue(collection.mutable.ListBuffer.empty)

def report(message: String, lineno: Int, level: String)(using failed: AtomicBoolean): Unit =
  failed.set(true)
  errors_and_warnings(lineno) += ((level, message))
  if debug then println(s"$level at line $lineno: $message")

def report_error(message: String, lineno: Int)(using AtomicBoolean): Unit =
  report(message, lineno, "error")

def report_warn(message: String, lineno: Int)(using AtomicBoolean): Unit =
  report(message, lineno, "warn")

def print_errors_and_warnings(): Unit =
  for (i, line) <- errors_and_warnings.toSeq.sortBy(_._1) do
    val tab = if i < 10 then "  " else if i < 100 then " " else ""
    println(s"Line $tab$i:")
    for (level, msg) <- line do println(s"\t$level: $msg")

extension [A, CC[X], C](col: collection.IterableOnceOps[A | Null, CC, C])
  inline def collectNonNullable = col.collect { case a: A => a }

class CompilerException(message: String, line: Int) extends Exception(message + s" at line $line")
