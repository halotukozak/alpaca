package example

import alpaca.internal.lexer.LazyReader

import java.nio.file.Files
import java.nio.file.Path

import scala.util.Using

@main
def main = Files
  .list(Path.of("in"))
  .forEach: file =>
    val tokens = Using.resource(LazyReader.from(file))(MatrixLexer.tokenize)
    val ast = MatrixParser.parse(tokens)
    print(ast)

//#     def quit_if_failed(self: object) -> None:
//#         if getattr(self, 'failed', False):
//#             print_errors_and_warnings()
//#             sys.exit(0)
//#
//#
//#     Utils.debug = False
//#
//#     print_ast = False
//#
//#     text = file.read()
//#     lexer = MatrixScanner()
//#     tokens = lexer.tokenize(text)
//#     quit_if_failed(lexer)
//#
//#     parser = MatrixParser()
//#     ast = parser.parse(tokens)
//#     quit_if_failed(parser)
//#
//#     if print_ast:
//#         treePrinter = TreePrinter()
//#         treePrinter.print_result(ast)
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
