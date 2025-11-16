# mypy: disable-error-code="no-untyped-def"

from collections import defaultdict

debug: bool

errors_and_warnings: dict[int, list] = defaultdict(lambda: [])


def report(message: str, lineno: int, level: str):
    errors_and_warnings[lineno].append((level, message))
    if debug:
        print(f"{level} at line {lineno}: {message}")


def report_error(self, message: str, lineno: int):
    self.failed = True
    report(message, lineno, "error")


def report_warn(self, message: str, lineno: int):
    report(message, lineno, "warn")


def print_errors_and_warnings():
    for i, line in sorted(errors_and_warnings.items()):
        tab = "  " if i < 10 else " " if i < 100 else ""
        print(f"Line {tab}{i}:")
        for level, msg in line:
            print(f"\t{level}: {msg}")


def addToClass(cls):
    def decorator(func):
        setattr(cls, func.__name__, func)
        return func

    return decorator


import inspect


def on(param_name):
    def f(fn):
        dispatcher = Dispatcher(param_name, fn)
        return dispatcher

    return f


def when(param_type):
    def f(fn):
        frame = inspect.currentframe().f_back
        func_name = fn.func_name if 'func_name' in dir(fn) else fn.__name__
        dispatcher = frame.f_locals[func_name]
        if not isinstance(dispatcher, Dispatcher):
            dispatcher = dispatcher.dispatcher
        dispatcher.add_target(param_type, fn)

        def ff(*args, **kw):
            return dispatcher(*args, **kw)

        ff.dispatcher = dispatcher
        return ff

    return f


class Dispatcher(object):
    def __init__(self, param_name, fn):
        self.param_index = self.__argspec(fn).args.index(param_name)
        self.param_name = param_name
        self.targets = {}

    def __call__(self, *args, **kw):
        typ = args[self.param_index].__class__
        d = self.targets.get(typ)
        if d is not None:
            return d(*args, **kw)
        else:
            issub = issubclass
            t = self.targets
            ks = iter(t)
            return [t[k](*args, **kw) for k in ks if issub(typ, k)]

    def add_target(self, typ, target):
        self.targets[typ] = target

    @staticmethod
    def __argspec(fn):
        # Support for Python 3 type hints requires inspect.getfullargspec
        if hasattr(inspect, 'getfullargspec'):
            return inspect.getfullargspec(fn)
        else:
            return inspect.getargspec(fn)
