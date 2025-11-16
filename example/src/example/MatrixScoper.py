from typing import Sequence

import AST
from AST import *
from Result import Success, Warn, Failure, Result
from SymbolTable import SymbolTable
from TypeSystem import AnyOf
from Utils import report_error, report_warn


class MatrixScoper:
    symbol_table = SymbolTable()

    def add_to_current_scope(self, symbol: SymbolRef) -> None:
        existing_symbol = self.get_symbol(symbol.name)
        if existing_symbol is not None and existing_symbol.type != symbol.type:
            report_warn(self,
                        f"Redeclaration of {symbol.name} : {existing_symbol.type} with new {symbol.type} type.",
                        symbol.lineno or -1,
                        )
        scope = self.symbol_table.actual_scope
        scope.symbols[symbol.name] = symbol

    def create_scope(self, tree: AST.Tree, in_loop: Optional[bool] = None) -> None:
        key = id(tree)
        new_scope = self.symbol_table.Scope(self.symbol_table.actual_scope, key, in_loop)
        self.symbol_table.actual_scope.children[key] = new_scope
        self.symbol_table.push_scope(tree)

    def get_symbol(self, name: str) -> Optional[SymbolRef]:
        return self.symbol_table.get_symbol(name)

    def pop_scope(self) -> None:
        self.symbol_table.pop_scope()

    def visit(self, node: Any) -> Any:
        method = 'visit_' + node.__class__.__name__
        visitor = getattr(self, method, self.generic_visit)
        return visitor(node)

    @staticmethod
    def generic_visit(node: Any) -> None:
        print(f"MatrixScoper: No visit_{node.__class__.__name__} method")

    def visit_all(self, tree: Sequence[Statement]) -> None:
        for node in tree:
            self.visit(node)

    def visit_If(self, if_: If) -> None:
        self.visit(if_.condition)
        self.create_scope(if_.then)
        self.visit(if_.then)
        self.pop_scope()
        if if_.else_:
            self.create_scope(if_.else_)
            self.visit(if_.else_)
            self.pop_scope()

    def visit_While(self, while_: While) -> None:
        self.visit(while_.condition)
        self.create_scope(while_.body, in_loop=True)
        self.visit(while_.body)
        self.pop_scope()

    def visit_For(self, for_: For) -> None:
        self.visit(for_.range)
        self.create_scope(for_.body, in_loop=True)
        self.add_to_current_scope(for_.var)
        self.visit(for_.body)
        self.pop_scope()

    def visit_Break(self, break_: Break) -> None:
        if not self.symbol_table.actual_scope.in_loop:
            report_error(self, "Break outside loop", break_.lineno)

    def visit_Continue(self, continue_: Continue) -> None:
        if not self.symbol_table.actual_scope.in_loop:
            report_error(self, "Continue outside loop", continue_.lineno)

    def visit_SymbolRef(self, ref: SymbolRef) -> None:
        symbol = self.get_symbol(ref.name)
        if symbol is None:
            report_error(self, f"Undefined variable {ref.name}", ref.lineno)
        else:
            ref.type = symbol.type

    def visit_MatrixRef(self, ref: MatrixRef) -> None:
        self.visit(ref.matrix)

    def visit_VectorRef(self, ref: VectorRef) -> None:
        self.visit(ref.vector)

    def visit_Assign(self, assign: Assign) -> None:
        self.visit(assign.expr)
        if isinstance(assign.var, SymbolRef):
            assign.var.type = assign.expr.type
            symbol = self.get_symbol(assign.var.name)
            if symbol is None:
                self.add_to_current_scope(assign.var)
            else:
                symbol.type = assign.var.type

    def visit_Apply(self, apply: Apply) -> None:
        self.visit(apply.ref)
        self.visit_all(apply.args)
        arg_types = [arg.type for arg in apply.args]

        if not isinstance(apply.ref, SymbolRef):
            raise NotImplementedError

        if isinstance(apply.ref.type, AnyOf):
            apply.ref.type = next(
                (type_ for type_ in apply.ref.type.all if
                 isinstance(type_, TS.Function) and type_.takes(arg_types)),
                TS.undef()
            )
            if apply.ref.type == TS.undef():
                apply.type = TS.undef()
            elif isinstance(apply.ref.type, TS.Function):
                if not apply.ref.type.result.is_final:
                    assert isinstance(apply.ref.type, TS.FunctionTypeFactory)
                    apply.ref.type = self.handle_result(apply.ref.type(apply.args), apply.lineno)
                assert isinstance(apply.ref.type, TS.Function)
                apply.type = apply.ref.type.result
            else:
                raise NotImplementedError
        elif isinstance(apply.ref.type, TS.Function):
            if not apply.ref.type.takes(arg_types):
                apply.type = TS.undef()
            else:
                if not apply.ref.type.result.is_final:
                    assert isinstance(apply.ref.type, TS.FunctionTypeFactory)
                    apply.ref.type = self.handle_result(apply.ref.type(apply.args), apply.lineno)
                assert isinstance(apply.ref.type, TS.Function)
                apply.type = apply.ref.type.result
        else:
            raise NotImplementedError

    def visit_Range(self, range_: Range) -> None:
        self.visit(range_.start)
        self.visit(range_.end)
        if isinstance(range_.start, SymbolRef) and range_.start.type != TS.Int():
            report_error(self, f"Expected Int, got {range_.start.type}", range_.lineno)
        if isinstance(range_.end, SymbolRef) and range_.end.type != TS.Int():
            report_error(self, f"Expected Int, got {range_.end.type}", range_.lineno)

    def visit_Literal(self, literal: Literal) -> None:
        pass

    def visit_Return(self, return_: Return) -> None:
        self.visit(return_.expr)

    def visit_Block(self, block: Block) -> None:
        self.visit_all(block.statements)

    def handle_result(self, result: Result[TS.Type], lineno: int) -> TS.Type:
        match result:
            case Success():
                pass
            case Warn(_, warns):
                for warn in warns:
                    report_warn(self, warn, lineno)
            case Failure(_, errors):
                for error in errors:
                    report_error(self, error, lineno)
        return result.value
