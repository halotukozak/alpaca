package example

import alpaca.internal.lexer.LazyReader
import alpaca.*

import java.nio.file.Files
import java.nio.file.Path
import scala.util.Using
import example.TreePrinter.printTree
import scala.util.control.NonFatal

@main
def main = Files
  .list(Path.of("example/src/example/in"))
  .forEach: file =>
    try
      println(s"Processing file: $file")
      val tokens = Using.resource(LazyReader.from(file))(MatrixLexer.tokenize)
      val (_, ast) = MatrixParser.parse(tokens)
      // ast.nn.printTree()

      val globalScope = Scope(null, ast.nn, false, symbols, Map.empty)
      val scoped = MatrixScoper.visit(ast.nn)(globalScope)

    catch
      case NonFatal(e) =>
        println(s"Error processing file $file: ${e.getMessage}")

//#
//#     scoper = MatrixScoper()
//#     scoper.visit_all(ast)
//#     # print(ast)
//#     type_checker = MatrixTypeChecker()
//#     type_checker.visit_all(ast)
//#     # print(ast)
//#     quit_if_failed(type_checker)
//#
//#     interpreter = MatrixInterpreter()
//#     interpreter.eval_all(ast)
