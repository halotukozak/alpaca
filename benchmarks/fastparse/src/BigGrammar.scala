package bench.fastparse

import fastparse.*
import MultiLineWhitespace.*

// Synthetic big-grammar parser for Fastparse matching the Alpaca BigGrammar structure.
// Parses whitespace-separated keywords (ka-kz, la-ld) and integers.
// Token names match the Alpaca BigGrammar to share generated input files.
object BigGrammarParser extends Parser[Any]:

  // --- Token parsers (30 keyword tokens + numbers) ---
  def ka[$: P]: P[String] = P("ka").map(_ => "ka")
  def kb[$: P]: P[String] = P("kb").map(_ => "kb")
  def kc[$: P]: P[String] = P("kc").map(_ => "kc")
  def kd[$: P]: P[String] = P("kd").map(_ => "kd")
  def ke[$: P]: P[String] = P("ke").map(_ => "ke")
  def kf[$: P]: P[String] = P("kf").map(_ => "kf")
  def kg[$: P]: P[String] = P("kg").map(_ => "kg")
  def kh[$: P]: P[String] = P("kh").map(_ => "kh")
  def ki[$: P]: P[String] = P("ki").map(_ => "ki")
  def kj[$: P]: P[String] = P("kj").map(_ => "kj")
  def kk[$: P]: P[String] = P("kk").map(_ => "kk")
  def kl[$: P]: P[String] = P("kl").map(_ => "kl")
  def km[$: P]: P[String] = P("km").map(_ => "km")
  def kn[$: P]: P[String] = P("kn").map(_ => "kn")
  def ko[$: P]: P[String] = P("ko").map(_ => "ko")
  def kp[$: P]: P[String] = P("kp").map(_ => "kp")
  def kq[$: P]: P[String] = P("kq").map(_ => "kq")
  def kr[$: P]: P[String] = P("kr").map(_ => "kr")
  def ks[$: P]: P[String] = P("ks").map(_ => "ks")
  def kt[$: P]: P[String] = P("kt").map(_ => "kt")
  def ku[$: P]: P[String] = P("ku").map(_ => "ku")
  def kv[$: P]: P[String] = P("kv").map(_ => "kv")
  def kw[$: P]: P[String] = P("kw").map(_ => "kw")
  def kx[$: P]: P[String] = P("kx").map(_ => "kx")
  def ky[$: P]: P[String] = P("ky").map(_ => "ky")
  def kz[$: P]: P[String] = P("kz").map(_ => "kz")
  def la[$: P]: P[String] = P("la").map(_ => "la")
  def lb[$: P]: P[String] = P("lb").map(_ => "lb")
  def lc[$: P]: P[String] = P("lc").map(_ => "lc")
  def ld[$: P]: P[String] = P("ld").map(_ => "ld")
  def num[$: P]: P[String] = P(CharIn("0-9").rep(1).!).map(_ => "num")

  // --- Compound rules: pairs of tokens (5 rules) ---
  def pairAB[$: P]: P[String] = P(ka ~ kb).map((a, b) => s"$a-$b")
  def pairCD[$: P]: P[String] = P(kc ~ kd).map((a, b) => s"$a-$b")
  def pairEF[$: P]: P[String] = P(ke ~ kf).map((a, b) => s"$a-$b")
  def pairGH[$: P]: P[String] = P(kg ~ kh).map((a, b) => s"$a-$b")
  def pairIJ[$: P]: P[String] = P(ki ~ kj).map((a, b) => s"$a-$b")

  // --- Compound rules: triples of tokens (5 rules) ---
  def tripleABC[$: P]: P[String] = P(ka ~ kb ~ kc).map((a, b, c) => s"$a-$b-$c")
  def tripleDEF[$: P]: P[String] = P(kd ~ ke ~ kf).map((a, b, c) => s"$a-$b-$c")
  def tripleGHI[$: P]: P[String] = P(kg ~ kh ~ ki).map((a, b, c) => s"$a-$b-$c")
  def tripleJKL[$: P]: P[String] = P(kj ~ kk ~ kl).map((a, b, c) => s"$a-$b-$c")
  def tripleMNO[$: P]: P[String] = P(km ~ kn ~ ko).map((a, b, c) => s"$a-$b-$c")

  // --- Composite rules: groups of alternative tokens (6 rules) ---
  def groupLow[$: P]: P[String] = P(ka | kb | kc | kd | ke)
  def groupMid[$: P]: P[String] = P(kf | kg | kh | ki | kj)
  def groupHigh[$: P]: P[String] = P(kk | kl | km | kn | ko)
  def groupTop[$: P]: P[String] = P(kp | kq | kr | ks | kt)
  def groupExtra[$: P]: P[String] = P(ku | kv | kw | kx | ky)
  def groupFinal[$: P]: P[String] = P(kz | la | lb | lc | ld)

  // --- Compound alternatives: any pair or triple ---
  def anyPair[$: P]: P[String] = P(pairAB | pairCD | pairEF | pairGH | pairIJ)
  def anyTriple[$: P]: P[String] = P(tripleABC | tripleDEF | tripleGHI | tripleJKL | tripleMNO)

  // --- Wider composite: any single token ---
  def anyToken[$: P]: P[String] = P(groupLow | groupMid | groupHigh | groupTop | groupExtra | groupFinal)

  // --- Statement: a token, pair, triple, or number ---
  def statement[$: P]: P[Any] = P(anyTriple | anyPair | anyToken | num)

  // --- Root expression: a single statement ---
  def expr[$: P]: P[Any] = P(statement ~ End)

  def parse(input: String): Either[String, Any] =
    fastparse.parse(input, expr(using _)) match
      case Parsed.Success(value, _) => Right(value)
      case f: Parsed.Failure => Left(f.msg)
