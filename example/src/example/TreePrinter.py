# mypy: disable-error-code="no-redef"

from typing import Optional

import AST
from Utils import addToClass


class TreePrinter:
    @staticmethod
    def safe_print_tree(obj: Optional[AST.Tree], indent_level: int) -> None:
        """Helper function to handle primitives and objects with `print_tree`."""
        prefix = "|  " * indent_level
        if obj is not None:
            obj.print_tree(indent_level)
        else:
            print(f"{prefix}None")

    @addToClass(AST.Assign)
    def print_tree(self: AST.Assign, indent_level=0) -> None:
        print("|  " * indent_level + "=")
        TreePrinter.safe_print_tree(self.var, indent_level + 1)
        TreePrinter.safe_print_tree(self.expr, indent_level + 1)

    @addToClass(AST.If)
    def print_tree(self: AST.If, indent_level=0) -> None:
        print("|  " * indent_level + "IF")
        TreePrinter.safe_print_tree(self.condition, indent_level + 1)
        print("|  " * indent_level + "THEN")
        for stmt in self.then:
            stmt.print_tree(indent_level + 1)
        if self.else_:
            print("|  " * indent_level + "ELSE")
            for stmt in self.else_:
                stmt.print_tree(indent_level + 1)

    @addToClass(AST.While)
    def print_tree(self: AST.While, indent_level=0) -> None:
        print("|  " * indent_level + "WHILE")
        print("|  " * (indent_level + 1) + "CONDITION")
        TreePrinter.safe_print_tree(self.condition, indent_level + 2)
        print("|  " * (indent_level + 1) + "BODY")
        for stmt in self.body:
            stmt.print_tree(indent_level + 2)

    @addToClass(AST.For)
    def print_tree(self: AST.For, indent_level=0) -> None:
        print("|  " * indent_level + "FOR")
        print("|  " * (indent_level + 1) + self.var.name)
        print("|  " * (indent_level + 1) + "RANGE")
        TreePrinter.safe_print_tree(self.range.start, indent_level + 2)
        TreePrinter.safe_print_tree(self.range.end, indent_level + 2)
        print("|  " * (indent_level + 1) + "BODY")
        for stmt in self.body:
            stmt.print_tree(indent_level + 2)

    @addToClass(AST.Apply)
    def print_tree(self: AST.Apply, indent_level=0) -> None:
        assert isinstance(self.ref, AST.SymbolRef)
        print("|  " * indent_level + f"{self.ref.name}")
        print("|  " * (indent_level + 1) + "ARGUMENTS")
        for arg in self.args:
            TreePrinter.safe_print_tree(arg, indent_level + 2)

    @addToClass(AST.Range)
    def print_tree(self: AST.Range, indent_level=0) -> None:
        print("|  " * indent_level + "RANGE")
        TreePrinter.safe_print_tree(self.start, indent_level + 1)
        TreePrinter.safe_print_tree(self.end, indent_level + 1)

    @addToClass(AST.SymbolRef)
    def print_tree(self: AST.SymbolRef, indent_level=0) -> None:
        print("|  " * indent_level + self.name)

    @addToClass(AST.VectorRef)
    def print_tree(self: AST.VectorRef, indent_level=0) -> None:
        print("|  " * indent_level + "VECTORREF")
        print("|  " * (indent_level + 1) + "ARGUMENTS")
        TreePrinter.safe_print_tree(self.vector, indent_level + 2)
        TreePrinter.safe_print_tree(self.element, indent_level + 2)

    @addToClass(AST.MatrixRef)
    def print_tree(self: AST.MatrixRef, indent_level=0) -> None:
        print("|  " * indent_level + "MATRIXREF")
        print("|  " * (indent_level + 1) + "ARGUMENTS")
        TreePrinter.safe_print_tree(self.matrix, indent_level + 2)
        TreePrinter.safe_print_tree(self.row, indent_level + 2)
        TreePrinter.safe_print_tree(self.col, indent_level + 2)

    @addToClass(AST.Literal)
    def print_tree(self: AST.Literal, indent_level=0) -> None:
        print("|  " * indent_level + str(self.value))

    @addToClass(AST.Break)
    def print_tree(self: AST.Break, indent_level=0) -> None:
        print("|  " * indent_level + "BREAK")

    @addToClass(AST.Continue)
    def print_tree(self: AST.Continue, indent_level=0) -> None:
        print("|  " * indent_level + "CONTINUE")

    @addToClass(AST.Return)
    def print_tree(self: AST.Return, indent_level=0) -> None:
        print("|  " * indent_level + "RETURN")
        TreePrinter.safe_print_tree(self.expr, indent_level + 1)

    @staticmethod
    def print_result(result: list[AST.Statement]) -> None:
        for r in result:
            r.print_tree(0)
