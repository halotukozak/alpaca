//package alpaca.ebnf
//
//import alpaca.parser.*
//
//object Parser extends Parser {
//
//  val terminator: Rule[EBNF] =
//    case Lexer.`;`(_) => ???
//    case Lexer.`\\.`(_) => ???
//
//  val symbol: Rule[EBNF] =
//    case Lexer.`\\[`(_) => ???
//    case Lexer.`\\]`(_) => ???
//    case Lexer.`\\{`(_) => ???
//    case Lexer.`\\}`(_) => ???
//    case Lexer.`\\(`(_) => ???
//    case Lexer.`\\)`(_) => ???
//    case Lexer.`<`(_) => ???
//    case Lexer.`>`(_) => ???
//    case Lexer.`'`(_) => ???
//    case Lexer.`"`(_) => ???
//    case Lexer.`=`(_) => ???
//    case Lexer.`\\|`(_) => ???
//    case Lexer.`\\.`(_) => ???
//    case Lexer.`,`(_) => ???
//    case Lexer.`;`(_) => ???
//    case Lexer.`-`(_) => ???
//    case Lexer.`\\+`(_) => ???
//    case Lexer.`\\*`(_) => ???
//    case Lexer.`\\?`(_) => ???
//    case Lexer.`\n`(_) => ???
//    case Lexer.`\t`(_) => ???
//    case Lexer.`\r`(_) => ???
//    case Lexer.`\f`(_) => ???
//    case Lexer.`\b`(_) => ???
//
//  val character: Rule[EBNF] =
//    case Lexer.letter(_) => ???
//    case Lexer.digit(_) => ???
//    case symbol(_) => ???
//    case Lexer.underscore(_) => ???
//    case Lexer.space(_) => ???
//
//  val `character - '`: Rule[EBNF] =
//    case Lexer.letter(_) => ???
//    case Lexer.digit(_) => ???
//    case symbol(_) => ???
//    case Lexer.underscore(_) => ???
//    case Lexer.space(_) => ???
//    case Lexer.`\\[`(_) => ???
//    case Lexer.`\\]`(_) => ???
//    case Lexer.`\\{`(_) => ???
//    case Lexer.`\\}`(_) => ???
//    case Lexer.`\\(`(_) => ???
//    case Lexer.`\\)`(_) => ???
//    case Lexer.`<`(_) => ???
//    case Lexer.`>`(_) => ???
//    case Lexer.`"`(_) => ???
//    case Lexer.`=`(_) => ???
//    case Lexer.`\\|`(_) => ???
//    case Lexer.`\\.`(_) => ???
//    case Lexer.`,`(_) => ???
//    case Lexer.`;`(_) => ???
//    case Lexer.`-`(_) => ???
//    case Lexer.`\\+`(_) => ???
//    case Lexer.`\\*`(_) => ???
//    case Lexer.`\\?`(_) => ???
//    case Lexer.`\n`(_) => ???
//    case Lexer.`\t`(_) => ???
//    case Lexer.`\r`(_) => ???
//    case Lexer.`\f`(_) => ???
//    case Lexer.`\b`(_) => ???
//
//  val `character - "`: Rule[EBNF] =
//    case Lexer.letter(_) => ???
//    case Lexer.digit(_) => ???
//    case symbol(_) => ???
//    case Lexer.underscore(_) => ???
//    case Lexer.space(_) => ???
//    case Lexer.`\\[`(_) => ???
//    case Lexer.`\\]`(_) => ???
//    case Lexer.`\\{`(_) => ???
//    case Lexer.`\\}`(_) => ???
//    case Lexer.`\\(`(_) => ???
//    case Lexer.`\\)`(_) => ???
//    case Lexer.`<`(_) => ???
//    case Lexer.`>`(_) => ???
//    case Lexer.`'`(_) => ???
//    case Lexer.`=`(_) => ???
//    case Lexer.`\\|`(_) => ???
//    case Lexer.`\\.`(_) => ???
//    case Lexer.`,`(_) => ???
//    case Lexer.`;`(_) => ???
//    case Lexer.`-`(_) => ???
//    case Lexer.`\\+`(_) => ???
//    case Lexer.`\\*`(_) => ???
//    case Lexer.`\\?`(_) => ???
//    case Lexer.`\n`(_) => ???
//    case Lexer.`\t`(_) => ???
//    case Lexer.`\r`(_) => ???
//    case Lexer.`\f`(_) => ???
//    case Lexer.`\b`(_) => ???
//
//  val terminal: Rule[EBNF] =
//    case (Lexer.`'`(_), `character - '`(_), `character - '`.List(_), Lexer.`'`(_)) => ???
//    case (Lexer.`"`(_), `character - "`(_), `character - "`.List(_), Lexer.`"`(_)) => ???
//
//  val SLoop: Rule[EBNF] =
//    case Lexer.space(_) => ???
//    case Lexer.`\n`(_) => ???
//    case Lexer.`\t`(_) => ???
//    case Lexer.`\r`(_) => ???
//    case Lexer.`\f`(_) => ???
//    case Lexer.`\b`(_) => ???
//
//  val S: Rule[EBNF] =
//    case SLoop.List(_) => ???
//
//  val identifierLoop: Rule[EBNF] =
//    case Lexer.letter(_) => ???
//    case Lexer.digit(_) => ???
//    case Lexer.underscore(_) => ???
//
//  val identifier: Rule[EBNF] =
//    case (Lexer.letter(_), identifierLoop.List(_)) => ???
//
//  val term: Rule[EBNF] =
//    case (Lexer.`\\(`(_), S(_), rhs(_), S(_), Lexer.`\\)`(_)) => ???
//    case (Lexer.`\\[`(_), S(_), rhs(_), S(_), Lexer.`\\]`(_)) => ???
//    case (Lexer.`\\{`(_), S(_), rhs(_), S(_), Lexer.`\\}`(_)) => ???
//    case terminal(_) => ???
//    case identifier(_) => ???
//
//  val factor: Rule[EBNF] =
//    case (term(_), S(_), Lexer.`\\?`(_)) => ???
//    case (term(_), S(_), Lexer.`\\*`(_)) => ???
//    case (term(_), S(_), Lexer.`\\+`(_)) => ???
//    case (term(_), S(_), Lexer.`-`(_), S(_), term(_)) => ???
//    case (term(_), S(_)) => ???
//
//  val concatenationLoop: Rule[EBNF] =
//    case (S(_), factor(_), S(_), Lexer.`,`(_)) => ???
//    case (S(_), factor(_), S(_)) => ???
//
//  val concatenation: Rule[EBNF] =
//    case (concatenationLoop, concatenationLoop.List(_)) => ???
//
//  val alternationLoop: Rule[EBNF] =
//    case (S(_), concatenation(_), S(_), Lexer.`\\|`(_)) => ???
//    case (S(_), concatenation(_), S(_)) => ???
//
//  val alternation: Rule[EBNF] =
//    case (alternationLoop, alternationLoop.List(_)) => ???
//
//  val rhs: Rule[EBNF] =
//    case alternation(_) => ???
//
//  val lhs: Rule[EBNF] =
//    case identifier(_) => ???
//
//  val rule: Rule[EBNF] =
//    case (lhs(_), S(_), Lexer.`=`(_), S(_), rhs(_), S(_), terminator(_)) =>
//      ???
//
//  val rootLoop: Rule[EBNF] =
//    case (S(_), rule(_), S(_)) => ???
//
//  val root: Rule[EBNF] =
//    case rootLoop.List(_) => ???
//}
