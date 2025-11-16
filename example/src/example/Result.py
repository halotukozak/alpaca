# todo: can be simplified to tuple?
from typing import Callable, TypeVar

T = TypeVar('T', covariant=True)


class Result[T]:
    def __init__(self, value: T):
        self.value = value

    def map[U](self, f: Callable[[T], U]) -> 'Result':
        raise NotImplementedError


class Success[T](Result[T]):
    def __init__(self, value: T):
        super().__init__(value)

    def map[U](self, f: Callable[[T], U]) -> Result[U]:
        return Success(f(self.value))


class Warn[T](Result[T]):
    __match_args__ = ('value', 'warns')

    def __init__(self, value: T, *warns: str):
        super().__init__(value)
        self.warns = warns

    def map[U](self, f: Callable[[T], U]) -> Result[U]:
        return Warn(f(self.value), *self.warns)


class Failure[T](Result[T]):
    __match_args__ = ('value', 'errors')

    def __init__(self, value: T, *errors: str):
        super().__init__(value)
        self.errors = errors

    def map[U](self, f: Callable[[T], U]) -> Result[U]:
        return Failure(f(self.value), *self.errors)
