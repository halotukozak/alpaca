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
//    .list(Path.of("src/example/in"))
//    .toList
//    .asScala
  Option(Path.of("src/example/in/init.m"))
    .foreach: file =>
      try
        println(s"Processing file: $file")
        val (_, lexems) = Using.resource(LazyReader.from(file))(MatrixLexer.tokenize)
        val (_, ast) = MatrixParser.parse(lexems)
        TreePrinter.visitNullable(ast)(0)

        val globalScope = Scope(ast.nn, null, false, symbols)
        val scoped = MatrixScoper.visitNullable(ast)(globalScope).get

        val initialTypeEnv: TypeEnv = symbols.map((name, sym) => name -> sym.tpe)
        val (typed, _) = MatrixTyper.visitNullable(ast)(initialTypeEnv).get

        val globalEnv = Env(null, mutable.Map.empty, globalFunctions.to(mutable.Map))
        val result = MatrixInterpreter.visitNullable(typed)(globalEnv)

        println(s"Result: $result")
      catch
        case NonFatal(e) =>
          println(s"Error processing file $file: \n${e.getMessage}")
          e.printStackTrace()
