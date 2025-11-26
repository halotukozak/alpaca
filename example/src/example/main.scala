package example

import alpaca.*
import alpaca.internal.lexer.LazyReader

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NonFatal

@main
def main() =
//  Files
//    .list(Path.of("example/src/example/in"))
//    .toList
//    .asScala
  Option(Path.of("example/src/example/in/example3.m"))
    .foreach: file =>
      try
        println(s"Processing file: $file")
        val tokens = Using.resource(LazyReader.from(file))(MatrixLexer.tokenize)
        val (_, ast) = MatrixParser.parse(tokens)
        TreePrinter.printTree(ast.nn)()

        val globalScope = Scope(ast.nn, null, false, symbols)
        val scoped = MatrixScoper.visit(ast.nn)(globalScope).get
        val typed = MatrixTyper.visit(ast.nn)().get.nn
        val globalEnv = Env(null, mutable.Map.empty, globalFunctions.to(mutable.Map))
        val result = MatrixInterpreter.visit(typed)(globalEnv)

        println(s"Result: $result")
      catch
        case NonFatal(e) =>
          println(s"Error processing file $file: \n${e.getMessage}")
          e.printStackTrace()
