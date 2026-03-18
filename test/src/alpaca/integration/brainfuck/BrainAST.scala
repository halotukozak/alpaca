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
    case BrainAST.Next => mem.pointer = (mem.pointer + 1) & 0xff
    case BrainAST.Prev => mem.pointer = (mem.pointer - 1) & 0xff
    case BrainAST.Inc => mem.underlying(mem.pointer) = (mem.underlying(mem.pointer) + 1) & 0xff
    case BrainAST.Dec => mem.underlying(mem.pointer) = (mem.underlying(mem.pointer) - 1) & 0xff
    case BrainAST.Print => print(mem.underlying(mem.pointer).toChar)
    case BrainAST.Read => mem.underlying(mem.pointer) = scala.io.StdIn.readByte()
    case BrainAST.While(ops) => while mem.underlying(mem.pointer) != 0 do ops.foreach(_.eval(mem))
    case BrainAST.FunctionDef(name, ops) => mem.functions += (name -> ops)
    case BrainAST.FunctionCall(name) =>
      mem.functions.get(name) match
        case Some(ops) => ops.foreach(_.eval(mem))
        case _ => throw RuntimeException(s"Undefined function: $name")

class Memory(
  val underlying: Array[Int] = new Array(255),
  var pointer: Int = 0,
  val functions: mutable.Map[String, List[BrainAST]] = mutable.Map.empty,
)
