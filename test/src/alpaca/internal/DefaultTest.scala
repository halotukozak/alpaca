package alpaca.internal

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.reflect.ClassTag

final class DefaultTest extends AnyFunSuite:

  def f[T](using ev: T withDefault Int, ct: ClassTag[T]): String = ct.runtimeClass.getName

  test("Default.infer") {
    assert(f == "int")
  }

  test("Default.provide") {
    assert(f[String] == "java.lang.String")
  }
