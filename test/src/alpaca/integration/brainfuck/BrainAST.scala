package alpaca.integration.brainfuck

import scala.collection.mutable

enum BrainAST:
  case Root(ops: List[BrainAST])
  case While(ops: List[BrainAST])
  case FunctionDef(name: String, ops: List[BrainAST])
  case FunctionCall(name: String)
  case Next, Prev, Inc, Dec, Print, Read

  def eval(mem: Memory): Unit = this match
    case BrainAST.Root(ops) => ops.foreach(_.eval(mem))
    case BrainAST.Next => mem.pointer += 1
    case BrainAST.Prev => mem.pointer -= 1
    case BrainAST.Inc => mem.underlying(mem.pointer.toInt) += 1
    case BrainAST.Dec => mem.underlying(mem.pointer.toInt) -= 1
    case BrainAST.Print => print(mem.underlying(mem.pointer.toInt))
    case BrainAST.Read => mem.underlying(mem.pointer.toInt) = UByte(scala.io.StdIn.readChar().toInt)
    case BrainAST.While(ops) =>
      while mem.underlying(mem.pointer.toInt).toInt != 0 do ops.foreach(_.eval(mem))
    case BrainAST.FunctionDef(name, ops) => mem.functions += (name -> ops)
    case BrainAST.FunctionCall(name) =>
      mem.functions.get(name) match
        case Some(ops) => ops.foreach(_.eval(mem))
        case _ => throw RuntimeException(s"Undefined function: $name")

class Memory(
  val underlying: Array[UByte] = Array.fill(128)(UByte(0)),
  var pointer: UByte = UByte(0),
  val functions: mutable.Map[String, List[BrainAST]] = mutable.Map.empty,
)
