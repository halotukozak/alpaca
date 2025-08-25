package alpaca.core

import org.scalatest.funsuite.AnyFunSuiteLike

import scala.reflect.ClassTag

class DefaultTest extends AnyFunSuiteLike {

  def f[T](using ev: T := Int, ct: ClassTag[T]): String = ct.runtimeClass.getName

  test("Default.infer") {
    assert(f == "int")
  }

  test("Default.provide") {
    assert(f[String] == "java.lang.String")
  }
}
