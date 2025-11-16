# mypy: disable-error-code="no-redef"

from AST import *
from Environment import EnvTable, Env, Matrix
from Utils import on, when


class Error(object):
    class Break(Exception):
        pass

    class Continue(Exception):
        pass


class MatrixInterpreter:
    env_table = EnvTable()

    def push_new_env(self) -> None:
        self.env_table.push_env(Env(self.env_table.actual_env))

    def create_fresh_env(self) -> None:
        self.env_table.push_env(Env(self.env_table.global_env))

    def pop_env(self) -> None:
        self.env_table.pop_env()

    @property
    def current_env(self) -> Env:
        return self.env_table.actual_env

    @on('node')
    def eval(self, node):  # type: ignore
        raise NotImplementedError

    def eval_all(self, tree: list[Statement]) -> None:
        for node in tree:
            self.eval(node)

    @when(If)
    def eval(self, if_: If):
        if self.eval(if_.condition):
            self.push_new_env()
            self.eval(if_.then)
            self.pop_env()
        elif if_.else_:
            self.push_new_env()
            self.eval(if_.else_)
            self.pop_env()

    @when(While)
    def eval(self, while_: While):
        while self.eval(while_.condition):
            try:
                self.push_new_env()
                self.eval(while_.body)
            except Error.Break:
                break
            except Error.Continue:
                continue
            finally:
                self.pop_env()

    @when(For)
    def eval(self, for_: For):
        self.current_env.create(for_.var.name)
        for i in self.eval(for_.range):
            self.current_env.update(for_.var.name, i)
            try:
                self.push_new_env()
                self.eval(for_.body)
            except Error.Break:
                break
            except Error.Continue:
                continue
            finally:
                self.pop_env()

    @when(Break)
    def eval(self, break_: Break):
        raise Error.Break

    @when(Continue)
    def eval(self, continue_: Continue):
        raise Error.Continue

    @when(SymbolRef)
    def eval(self, ref: SymbolRef):
        return self.current_env.get_value(ref.name)

    @when(MatrixRef)
    def eval(self, ref: MatrixRef) -> int | float:
        return self.current_env.get_value(ref.matrix.name)[self.eval(ref.row)][self.eval(ref.col)]

    @when(VectorRef)
    def eval(self, ref: VectorRef) -> int | float:
        return self.eval(ref.vector)[self.eval(ref.element)]

    @when(Assign)
    def eval(self, assign: Assign):
        res = self.eval(assign.expr)
        if isinstance(assign.var, SymbolRef):
            updated = self.current_env.update(assign.var.name, res)
            if not updated:
                self.current_env.create(assign.var.name, res)
        elif isinstance(assign.var, MatrixRef):
            matrix: Matrix = self.current_env.get_value(assign.var.matrix.name)
            matrix[self.eval(assign.var.row)][self.eval(assign.var.col)] = res
        elif isinstance(assign.var, VectorRef):
            vector = self.current_env.get_value(assign.var.vector.name)
            vector[self.eval(assign.var.element)] = res
        else:
            raise NotImplementedError

    @when(Apply)
    def eval(self, apply: Apply):
        if isinstance(apply.ref, SymbolRef):
            f = self.current_env.get_function(apply.ref.name)
            if f is None:
                raise NotImplementedError(f"Function {apply.ref.name} not implemented")
            return f(*(self.eval(arg) for arg in apply.args))
        else:
            raise NotImplementedError

    @when(Range)
    def eval(self, range_: Range) -> range:
        start: int = self.eval(range_.start)
        stop: int = self.eval(range_.end)
        return range(start, stop)

    @when(Literal)
    def eval(self, literal: Literal):
        return literal.value

    @when(Return)
    def eval(self, return_: Return):
        return self.eval(return_)

    @when(Block)
    def eval(self, block: Block):
        self.push_new_env()
        self.eval_all(block.statements)
        self.pop_env()
