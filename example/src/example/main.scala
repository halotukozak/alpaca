package example

import alpaca.*
import alpaca.internal.lexer.LazyReader

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NonFatal

@main
def main(): Unit =
  Files
    .list(Path.of("src/example/in"))
    .toList
    .asScala
    .foreach: file =>
      try
        println(s"Processing file: $file")
        val (_, lexems) = Using.resource(LazyReader.from(file))(MatrixLexer.tokenize)
        val (_, ast) = MatrixParser.parse(lexems)
        TreePrinter.visitNullable(ast)(0)
        val globalScope = Scope(
          ast.nn,
          null,
          false,
          runtime.Function.values
            .map: func =>
              (func.productPrefix, func.toRef)
            .toMap,
        )
        val scoped = MatrixScoper.visitNullable(ast)(globalScope).get
        val initialTypeEnv = runtime.Function.values
          .map: func =>
            (func.productPrefix, func.tpe)
          .toMap
        val (typed, _) = MatrixTyper.visitNullable(ast)(initialTypeEnv).get
        TreePrinter.visitNullable(typed)(0)
        val globalEnv = Env(
          null,
          mutable.Map.empty,
          runtime.Function.values
            .map:
              case runtime.Function(name, implementation) => name -> implementation
            .to(mutable.Map),
        )
        val result = MatrixInterpreter.visitNullable(typed)(globalEnv)
        println(s"Result: $result")
      catch
        case NonFatal(e) =>
          println(s"Error processing file $file: \n${e.getMessage}")
          e.printStackTrace()
