package bench.fastparse

trait Parser[T]:
  def parse(input: String): Either[String, T]
