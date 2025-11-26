package example

import alpaca.*

// @nowarn("msg=flexible type")
// val MatrixLexer = lexer:
//   case "\\+=" => Token["ADDASSIGN"]
//   case "-=" => Token["SUBASSIGN"]
//   case "\\*=" => Token["MULASSIGN"]
//   case "/=" => Token["DIVASSIGN"]

//   case "!=" => Token["NOT_EQUAL"]
//   case "<=" => Token["LESS_EQUAL"]
//   case ">=" => Token["GREATER_EQUAL"]
//   case "==" => Token["EQUAL"]

//   case literal @ ("<" | ">" | "=" | "\\+" | "-" | "\\*" | "/" | "\\(" | "\\)" | "\\[" | "\\]" | "\\{" | "\\}" | ":" |
//       "'" | "," | ";") =>
//     Token[literal.type](literal)

//   case "\\.\\+" => Token["DOTADD"]
//   case "\\.\\-" => Token["DOTSUB"]
//   case "\\.\\*" => Token["DOTMUL"]
//   case "\\./" => Token["DOTDIV"]

//   case keyword @ ("if" | "else" | "for" | "while" | "break" | "continue" | "return" | "eye" | "zeros" | "ones" |
//       "print") =>
//     Token[keyword.type](keyword)
//   case id @ "[a-zA-Z_][a-zA-Z0-9_]*" => Token["ID"](id)
//   case float @ "(\\d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?" => Token["FLOAT"](float.toDouble)
//   case int @ "[0-9]+" => Token["INTNUM"](int.toInt)
//   case string @ "\"[^\"]*\"" => Token["STRING"](string.substring(1, string.length - 1))
//   case (" " | "\t" | "\\#.*") => Token.Ignored
//   case newLines @ "\n+" =>
//     ctx.line += newLines.count(_ == '\n')
//     Token.Ignored

object MatrixLexer extends alpaca.internal.lexer.Tokenization[alpaca.LexerCtx.Default] {
  type Fields = NamedTuple.NamedTuple[
    *:[
      "STRING",
      *:[
        "INTNUM",
        *:[
          "FLOAT",
          *:[
            "ID",
            *:[
              "print",
              *:[
                "ones",
                *:[
                  "zeros",
                  *:[
                    "eye",
                    *:[
                      "return",
                      *:[
                        "continue",
                        *:[
                          "break",
                          *:[
                            "while",
                            *:[
                              "for",
                              *:[
                                "else",
                                *:[
                                  "if",
                                  *:[
                                    "DOTDIV",
                                    *:[
                                      "DOTMUL",
                                      *:[
                                        "DOTSUB",
                                        *:[
                                          "DOTADD",
                                          *:[
                                            ";",
                                            *:[
                                              ",",
                                              *:[
                                                "\'",
                                                *:[
                                                  ":",
                                                  *:[
                                                    "\\}",
                                                    *:[
                                                      "\\{",
                                                      *:[
                                                        "\\]",
                                                        *:[
                                                          "\\[",
                                                          *:[
                                                            "\\)",
                                                            *:[
                                                              "\\(",
                                                              *:[
                                                                "/",
                                                                *:[
                                                                  "\\*",
                                                                  *:[
                                                                    "-",
                                                                    *:[
                                                                      "\\+",
                                                                      *:[
                                                                        "=",
                                                                        *:[
                                                                          ">",
                                                                          *:[
                                                                            "<",
                                                                            *:[
                                                                              "EQUAL",
                                                                              *:[
                                                                                "GREATER_EQUAL",
                                                                                *:[
                                                                                  "LESS_EQUAL",
                                                                                  *:[
                                                                                    "NOT_EQUAL",
                                                                                    *:["DIVASSIGN", *:[
                                                                                      "MULASSIGN",
                                                                                      *:["SUBASSIGN", *:[
                                                                                        "ADDASSIGN",
                                                                                        Tuple$package.EmptyTuple,
                                                                                      ]],
                                                                                    ]],
                                                                                  ],
                                                                                ],
                                                                              ],
                                                                            ],
                                                                          ],
                                                                        ],
                                                                      ],
                                                                    ],
                                                                  ],
                                                                ],
                                                              ],
                                                            ],
                                                          ],
                                                        ],
                                                      ],
                                                    ],
                                                  ],
                                                ],
                                              ],
                                            ],
                                          ],
                                        ],
                                      ],
                                    ],
                                  ],
                                ],
                              ],
                            ],
                          ],
                        ],
                      ],
                    ],
                  ],
                ],
              ],
            ],
          ],
        ],
      ],
    ],
    *:[
      alpaca.internal.lexer.Token["STRING", alpaca.LexerCtx.Default, java.lang.String],
      *:[
        alpaca.internal.lexer.Token["INTNUM", alpaca.LexerCtx.Default, Int],
        *:[
          alpaca.internal.lexer.Token["FLOAT", alpaca.LexerCtx.Default, Double],
          *:[
            alpaca.internal.lexer.Token["ID", alpaca.LexerCtx.Default, java.lang.String],
            *:[
              alpaca.internal.lexer.Token["print", alpaca.LexerCtx.Default, java.lang.String],
              *:[
                alpaca.internal.lexer.Token["ones", alpaca.LexerCtx.Default, java.lang.String],
                *:[
                  alpaca.internal.lexer.Token["zeros", alpaca.LexerCtx.Default, java.lang.String],
                  *:[
                    alpaca.internal.lexer.Token["eye", alpaca.LexerCtx.Default, java.lang.String],
                    *:[
                      alpaca.internal.lexer.Token["return", alpaca.LexerCtx.Default, java.lang.String],
                      *:[
                        alpaca.internal.lexer.Token["continue", alpaca.LexerCtx.Default, java.lang.String],
                        *:[
                          alpaca.internal.lexer.Token["break", alpaca.LexerCtx.Default, java.lang.String],
                          *:[
                            alpaca.internal.lexer.Token["while", alpaca.LexerCtx.Default, java.lang.String],
                            *:[
                              alpaca.internal.lexer.Token["for", alpaca.LexerCtx.Default, java.lang.String],
                              *:[
                                alpaca.internal.lexer.Token["else", alpaca.LexerCtx.Default, java.lang.String],
                                *:[
                                  alpaca.internal.lexer.Token["if", alpaca.LexerCtx.Default, java.lang.String],
                                  *:[
                                    alpaca.internal.lexer.Token["DOTDIV", alpaca.LexerCtx.Default, Unit],
                                    *:[
                                      alpaca.internal.lexer.Token["DOTMUL", alpaca.LexerCtx.Default, Unit],
                                      *:[
                                        alpaca.internal.lexer.Token["DOTSUB", alpaca.LexerCtx.Default, Unit],
                                        *:[
                                          alpaca.internal.lexer.Token["DOTADD", alpaca.LexerCtx.Default, Unit],
                                          *:[
                                            alpaca.internal.lexer.Token[";", alpaca.LexerCtx.Default, java.lang.String],
                                            *:[
                                              alpaca.internal.lexer.Token[
                                                ",",
                                                alpaca.LexerCtx.Default,
                                                java.lang.String,
                                              ],
                                              *:[
                                                alpaca.internal.lexer.Token[
                                                  "\'",
                                                  alpaca.LexerCtx.Default,
                                                  java.lang.String,
                                                ],
                                                *:[
                                                  alpaca.internal.lexer.Token[
                                                    ":",
                                                    alpaca.LexerCtx.Default,
                                                    java.lang.String,
                                                  ],
                                                  *:[
                                                    alpaca.internal.lexer.Token[
                                                      "\\}",
                                                      alpaca.LexerCtx.Default,
                                                      java.lang.String,
                                                    ],
                                                    *:[
                                                      alpaca.internal.lexer.Token[
                                                        "\\{",
                                                        alpaca.LexerCtx.Default,
                                                        java.lang.String,
                                                      ],
                                                      *:[
                                                        alpaca.internal.lexer.Token[
                                                          "\\]",
                                                          alpaca.LexerCtx.Default,
                                                          java.lang.String,
                                                        ],
                                                        *:[
                                                          alpaca.internal.lexer.Token[
                                                            "\\[",
                                                            alpaca.LexerCtx.Default,
                                                            java.lang.String,
                                                          ],
                                                          *:[
                                                            alpaca.internal.lexer.Token[
                                                              "\\)",
                                                              alpaca.LexerCtx.Default,
                                                              java.lang.String,
                                                            ],
                                                            *:[
                                                              alpaca.internal.lexer.Token[
                                                                "\\(",
                                                                alpaca.LexerCtx.Default,
                                                                java.lang.String,
                                                              ],
                                                              *:[
                                                                alpaca.internal.lexer.Token[
                                                                  "/",
                                                                  alpaca.LexerCtx.Default,
                                                                  java.lang.String,
                                                                ],
                                                                *:[
                                                                  alpaca.internal.lexer.Token[
                                                                    "\\*",
                                                                    alpaca.LexerCtx.Default,
                                                                    java.lang.String,
                                                                  ],
                                                                  *:[
                                                                    alpaca.internal.lexer.Token[
                                                                      "-",
                                                                      alpaca.LexerCtx.Default,
                                                                      java.lang.String,
                                                                    ],
                                                                    *:[
                                                                      alpaca.internal.lexer.Token[
                                                                        "\\+",
                                                                        alpaca.LexerCtx.Default,
                                                                        java.lang.String,
                                                                      ],
                                                                      *:[
                                                                        alpaca.internal.lexer.Token[
                                                                          "=",
                                                                          alpaca.LexerCtx.Default,
                                                                          java.lang.String,
                                                                        ],
                                                                        *:[
                                                                          alpaca.internal.lexer.Token[
                                                                            ">",
                                                                            alpaca.LexerCtx.Default,
                                                                            java.lang.String,
                                                                          ],
                                                                          *:[
                                                                            alpaca.internal.lexer.Token[
                                                                              "<",
                                                                              alpaca.LexerCtx.Default,
                                                                              java.lang.String,
                                                                            ],
                                                                            *:[
                                                                              alpaca.internal.lexer.Token[
                                                                                "EQUAL",
                                                                                alpaca.LexerCtx.Default,
                                                                                Unit,
                                                                              ],
                                                                              *:[
                                                                                alpaca.internal.lexer.Token[
                                                                                  "GREATER_EQUAL",
                                                                                  alpaca.LexerCtx.Default,
                                                                                  Unit,
                                                                                ],
                                                                                *:[
                                                                                  alpaca.internal.lexer.Token[
                                                                                    "LESS_EQUAL",
                                                                                    alpaca.LexerCtx.Default,
                                                                                    Unit,
                                                                                  ],
                                                                                  *:[
                                                                                    alpaca.internal.lexer.Token[
                                                                                      "NOT_EQUAL",
                                                                                      alpaca.LexerCtx.Default,
                                                                                      Unit,
                                                                                    ],
                                                                                    *:[
                                                                                      alpaca.internal.lexer.Token[
                                                                                        "DIVASSIGN",
                                                                                        alpaca.LexerCtx.Default,
                                                                                        Unit,
                                                                                      ],
                                                                                      *:[
                                                                                        alpaca.internal.lexer.Token[
                                                                                          "MULASSIGN",
                                                                                          alpaca.LexerCtx.Default,
                                                                                          Unit,
                                                                                        ],
                                                                                        *:[
                                                                                          alpaca.internal.lexer.Token[
                                                                                            "SUBASSIGN",
                                                                                            alpaca.LexerCtx.Default,
                                                                                            Unit,
                                                                                          ],
                                                                                          *:[
                                                                                            alpaca.internal.lexer.Token[
                                                                                              "ADDASSIGN",
                                                                                              alpaca.LexerCtx.Default,
                                                                                              Unit,
                                                                                            ],
                                                                                            Tuple$package.EmptyTuple,
                                                                                          ],
                                                                                        ],
                                                                                      ],
                                                                                    ],
                                                                                  ],
                                                                                ],
                                                                              ],
                                                                            ],
                                                                          ],
                                                                        ],
                                                                      ],
                                                                    ],
                                                                  ],
                                                                ],
                                                              ],
                                                            ],
                                                          ],
                                                        ],
                                                      ],
                                                    ],
                                                  ],
                                                ],
                                              ],
                                            ],
                                          ],
                                        ],
                                      ],
                                    ],
                                  ],
                                ],
                              ],
                            ],
                          ],
                        ],
                      ],
                    ],
                  ],
                ],
              ],
            ],
          ],
        ],
      ],
    ],
  ]
  lazy val byName: collection.immutable.Map[
    java.lang.String,
    alpaca.internal.lexer.DefinedToken[? >: Nothing <: Any, alpaca.LexerCtx.Default, ? >: Nothing <: Any],
  ] = Predef.Map.apply[
    java.lang.String,
    alpaca.internal.lexer.DefinedToken[? >: Nothing <: Any, alpaca.LexerCtx.Default, ? >: Nothing <: Any],
  ](
    Tuple2.apply["ADDASSIGN", this.ADDASSIGN.type]("ADDASSIGN", this.ADDASSIGN),
    Tuple2.apply["SUBASSIGN", this.SUBASSIGN.type]("SUBASSIGN", this.SUBASSIGN),
    Tuple2.apply["MULASSIGN", this.MULASSIGN.type]("MULASSIGN", this.MULASSIGN),
    Tuple2.apply["DIVASSIGN", this.DIVASSIGN.type]("DIVASSIGN", this.DIVASSIGN),
    Tuple2.apply["NOT_EQUAL", this.NOT_EQUAL.type]("NOT_EQUAL", this.NOT_EQUAL),
    Tuple2.apply["LESS_EQUAL", this.LESS_EQUAL.type]("LESS_EQUAL", this.LESS_EQUAL),
    Tuple2.apply["GREATER_EQUAL", this.GREATER_EQUAL.type]("GREATER_EQUAL", this.GREATER_EQUAL),
    Tuple2.apply["EQUAL", this.EQUAL.type]("EQUAL", this.EQUAL),
    Tuple2.apply["<", this.<.type]("<", this.<),
    Tuple2.apply[">", this.>.type](">", this.>),
    Tuple2.apply["=", this.`=`.type]("=", this.`=`),
    Tuple2.apply["\\+", this.`\\+`.type]("\\+", this.`\\+`),
    Tuple2.apply["-", this.-.type]("-", this.-),
    Tuple2.apply["\\*", this.`\\*`.type]("\\*", this.`\\*`),
    Tuple2.apply["/", this.`/`.type]("/", this.`/`),
    Tuple2.apply["\\(", this.`\\(`.type]("\\(", this.`\\(`),
    Tuple2.apply["\\)", this.`\\)`.type]("\\)", this.`\\)`),
    Tuple2.apply["\\[", this.`\\[`.type]("\\[", this.`\\[`),
    Tuple2.apply["\\]", this.`\\]`.type]("\\]", this.`\\]`),
    Tuple2.apply["\\{", this.`\\{`.type]("\\{", this.`\\{`),
    Tuple2.apply["\\}", this.`\\}`.type]("\\}", this.`\\}`),
    Tuple2.apply[":", this.`:`.type](":", this.`:`),
    Tuple2.apply["\'", this.`\'`.type]("\'", this.`\'`),
    Tuple2.apply[",", this.`,`.type](",", this.`,`),
    Tuple2.apply[";", this.`;`.type](";", this.`;`),
    Tuple2.apply["DOTADD", this.DOTADD.type]("DOTADD", this.DOTADD),
    Tuple2.apply["DOTSUB", this.DOTSUB.type]("DOTSUB", this.DOTSUB),
    Tuple2.apply["DOTMUL", this.DOTMUL.type]("DOTMUL", this.DOTMUL),
    Tuple2.apply["DOTDIV", this.DOTDIV.type]("DOTDIV", this.DOTDIV),
    Tuple2.apply["if", this.`if`.type]("if", this.`if`),
    Tuple2.apply["else", this.`else`.type]("else", this.`else`),
    Tuple2.apply["for", this.`for`.type]("for", this.`for`),
    Tuple2.apply["while", this.`while`.type]("while", this.`while`),
    Tuple2.apply["break", this.`break`.type]("break", this.break),
    Tuple2.apply["continue", this.continue.type]("continue", this.continue),
    Tuple2.apply["return", this.`return`.type]("return", this.`return`),
    Tuple2.apply["eye", this.eye.type]("eye", this.eye),
    Tuple2.apply["zeros", this.zeros.type]("zeros", this.zeros),
    Tuple2.apply["ones", this.ones.type]("ones", this.ones),
    Tuple2.apply["print", this.print.type]("print", this.print),
    Tuple2.apply["ID", this.ID.type]("ID", this.ID),
    Tuple2.apply["FLOAT", this.FLOAT.type]("FLOAT", this.FLOAT),
    Tuple2.apply["INTNUM", this.INTNUM.type]("INTNUM", this.INTNUM),
    Tuple2.apply["STRING", this.STRING.type]("STRING", this.STRING),
  )

  override protected val compiled: util.matching.Regex = new util.matching.Regex(
    "(?<token0>\\+=)|(?<token1>-=)|(?<token2>\\*=)|(?<token3>/=)|(?<token4>!=)|(?<token5><=)|(?<token6>>=)|(?<token7>==)|(?<token8><)|(?<token9>>)|(?<token10>=)|(?<token11>\\+)|(?<token12>-)|(?<token13>\\*)|(?<token14>/)|(?<token15>\\()|(?<token16>\\))|(?<token17>\\[)|(?<token18>\\])|(?<token19>\\{)|(?<token20>\\})|(?<token21>:)|(?<token22>\\')|(?<token23>,)|(?<token24>;)|(?<token25>\\.\\+)|(?<token26>\\.\\-)|(?<token27>\\.\\*)|(?<token28>\\./)|(?<token29>if)|(?<token30>else)|(?<token31>for)|(?<token32>while)|(?<token33>break)|(?<token34>continue)|(?<token35>return)|(?<token36>eye)|(?<token37>zeros)|(?<token38>ones)|(?<token39>print)|(?<token40>[a-zA-Z_][a-zA-Z0-9_]*)|(?<token41>(\\d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?)|(?<token42>[0-9]+)|(?<token43>\"[^\"]*\")|(?<token44> )|(?<token45>\t)|(?<token46>\\#.*)|(?<token47>\n+)",
  )

  val ADDASSIGN: alpaca.internal.lexer.DefinedToken["ADDASSIGN", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["ADDASSIGN", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["ADDASSIGN"]("ADDASSIGN", "token0", "\\+="),
      (_$3: alpaca.LexerCtx.Default) => (),
      (_$1: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val SUBASSIGN: alpaca.internal.lexer.DefinedToken["SUBASSIGN", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["SUBASSIGN", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["SUBASSIGN"]("SUBASSIGN", "token1", "-="),
      (`_$3₂`: alpaca.LexerCtx.Default) => (),
      (`_$1₂`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val MULASSIGN: alpaca.internal.lexer.DefinedToken["MULASSIGN", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["MULASSIGN", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["MULASSIGN"]("MULASSIGN", "token2", "\\*="),
      (`_$3₃`: alpaca.LexerCtx.Default) => (),
      (`_$1₃`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val DIVASSIGN: alpaca.internal.lexer.DefinedToken["DIVASSIGN", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["DIVASSIGN", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["DIVASSIGN"]("DIVASSIGN", "token3", "/="),
      (`_$3₄`: alpaca.LexerCtx.Default) => (),
      (`_$1₄`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val NOT_EQUAL: alpaca.internal.lexer.DefinedToken["NOT_EQUAL", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["NOT_EQUAL", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["NOT_EQUAL"]("NOT_EQUAL", "token4", "!="),
      (`_$3₅`: alpaca.LexerCtx.Default) => (),
      (`_$1₅`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val LESS_EQUAL: alpaca.internal.lexer.DefinedToken["LESS_EQUAL", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["LESS_EQUAL", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["LESS_EQUAL"]("LESS_EQUAL", "token5", "<="),
      (`_$3₆`: alpaca.LexerCtx.Default) => (),
      (`_$1₆`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val GREATER_EQUAL: alpaca.internal.lexer.DefinedToken["GREATER_EQUAL", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["GREATER_EQUAL", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["GREATER_EQUAL"]("GREATER_EQUAL", "token6", ">="),
      (`_$3₇`: alpaca.LexerCtx.Default) => (),
      (`_$1₇`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val EQUAL: alpaca.internal.lexer.DefinedToken["EQUAL", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["EQUAL", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["EQUAL"]("EQUAL", "token7", "=="),
      (`_$3₈`: alpaca.LexerCtx.Default) => (),
      (`_$1₈`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val `<`: alpaca.internal.lexer.DefinedToken["<", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["<", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["<"]("<", "token8", "<"),
      (`_$3₉`: alpaca.LexerCtx.Default) => (),
      (_$2: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => _$2.lastRawMatched,
    )
  val `>`: alpaca.internal.lexer.DefinedToken[">", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply[">", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply[">"](">", "token9", ">"),
      (`_$3₁₀`: alpaca.LexerCtx.Default) => (),
      (`_$2₂`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂`.lastRawMatched,
    )
  val `=`: alpaca.internal.lexer.DefinedToken["=", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["=", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["="]("=", "token10", "="),
      (`_$3₁₁`: alpaca.LexerCtx.Default) => (),
      (`_$2₃`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₃`.lastRawMatched,
    )
  val `\\+`: alpaca.internal.lexer.DefinedToken["\\+", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\+", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\+"]("\\+", "token11", "\\+"),
      (`_$3₁₂`: alpaca.LexerCtx.Default) => (),
      (`_$2₄`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₄`.lastRawMatched,
    )
  val `-`: alpaca.internal.lexer.DefinedToken["-", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["-", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["-"]("-", "token12", "-"),
      (`_$3₁₃`: alpaca.LexerCtx.Default) => (),
      (`_$2₅`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₅`.lastRawMatched,
    )
  val `\\*`: alpaca.internal.lexer.DefinedToken["\\*", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\*", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\*"]("\\*", "token13", "\\*"),
      (`_$3₁₄`: alpaca.LexerCtx.Default) => (),
      (`_$2₆`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₆`.lastRawMatched,
    )
  val `/`: alpaca.internal.lexer.DefinedToken["/", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["/", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["/"]("/", "token14", "/"),
      (`_$3₁₅`: alpaca.LexerCtx.Default) => (),
      (`_$2₇`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₇`.lastRawMatched,
    )
  val `\\(`: alpaca.internal.lexer.DefinedToken["\\(", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\(", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\("]("\\(", "token15", "\\("),
      (`_$3₁₆`: alpaca.LexerCtx.Default) => (),
      (`_$2₈`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₈`.lastRawMatched,
    )
  val `\\)`: alpaca.internal.lexer.DefinedToken["\\)", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\)", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\)"]("\\)", "token16", "\\)"),
      (`_$3₁₇`: alpaca.LexerCtx.Default) => (),
      (`_$2₉`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₉`.lastRawMatched,
    )
  val `\\[`: alpaca.internal.lexer.DefinedToken["\\[", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\[", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\["]("\\[", "token17", "\\["),
      (`_$3₁₈`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₀`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₀`.lastRawMatched,
    )
  val `\\]`: alpaca.internal.lexer.DefinedToken["\\]", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\]", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\]"]("\\]", "token18", "\\]"),
      (`_$3₁₉`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₁`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₁`.lastRawMatched,
    )
  val `\\{`: alpaca.internal.lexer.DefinedToken["\\{", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\{", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\{"]("\\{", "token19", "\\{"),
      (`_$3₂₀`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₂`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₂`.lastRawMatched,
    )
  val `\\}`: alpaca.internal.lexer.DefinedToken["\\}", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["\\}", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\\}"]("\\}", "token20", "\\}"),
      (`_$3₂₁`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₃`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₃`.lastRawMatched,
    )
  val `:`: alpaca.internal.lexer.DefinedToken[":", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply[":", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply[":"](":", "token21", ":"),
      (`_$3₂₂`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₄`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₄`.lastRawMatched,
    )
  val `\'`: alpaca.internal.lexer.DefinedToken["\'", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["'", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["\'"]("\'", "token22", "\'"),
      (_: alpaca.LexerCtx.Default) => (),
      (x: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => x.lastRawMatched,
    )
  val `,`: alpaca.internal.lexer.DefinedToken[",", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply[",", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply[","](",", "token23", ","),
      (`_$3₂₄`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₆`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₆`.lastRawMatched,
    )
  val `;`: alpaca.internal.lexer.DefinedToken[";", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply[";", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply[";"](";", "token24", ";"),
      (`_$3₂₅`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₇`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₇`.lastRawMatched,
    )
  val DOTADD: alpaca.internal.lexer.DefinedToken["DOTADD", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["DOTADD", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["DOTADD"]("DOTADD", "token25", "\\.\\+"),
      (`_$3₂₆`: alpaca.LexerCtx.Default) => (),
      (`_$1₉`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val DOTSUB: alpaca.internal.lexer.DefinedToken["DOTSUB", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["DOTSUB", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["DOTSUB"]("DOTSUB", "token26", "\\.\\-"),
      (`_$3₂₇`: alpaca.LexerCtx.Default) => (),
      (`_$1₁₀`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val DOTMUL: alpaca.internal.lexer.DefinedToken["DOTMUL", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["DOTMUL", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["DOTMUL"]("DOTMUL", "token27", "\\.\\*"),
      (`_$3₂₈`: alpaca.LexerCtx.Default) => (),
      (`_$1₁₁`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val DOTDIV: alpaca.internal.lexer.DefinedToken["DOTDIV", alpaca.LexerCtx.Default, Unit] =
    alpaca.internal.lexer.DefinedToken.apply["DOTDIV", alpaca.LexerCtx.Default, Unit](
      alpaca.internal.lexer.TokenInfo.apply["DOTDIV"]("DOTDIV", "token28", "\\./"),
      (`_$3₂₉`: alpaca.LexerCtx.Default) => (),
      (`_$1₁₂`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => (),
    )
  val `if`: alpaca.internal.lexer.DefinedToken["if", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["if", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["if"]("if", "token29", "if"),
      (`_$3₃₀`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₈`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₈`.lastRawMatched,
    )
  val `else`: alpaca.internal.lexer.DefinedToken["else", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["else", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["else"]("else", "token30", "else"),
      (`_$3₃₁`: alpaca.LexerCtx.Default) => (),
      (`_$2₁₉`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₁₉`.lastRawMatched,
    )
  val `for`: alpaca.internal.lexer.DefinedToken["for", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["for", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["for"]("for", "token31", "for"),
      (`_$3₃₂`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₀`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₀`.lastRawMatched,
    )
  val `while`: alpaca.internal.lexer.DefinedToken["while", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["while", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["while"]("while", "token32", "while"),
      (`_$3₃₃`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₁`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₁`.lastRawMatched,
    )
  val `break`: alpaca.internal.lexer.DefinedToken["break", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["break", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["break"]("break", "token33", "break"),
      (`_$3₃₄`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₂`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₂`.lastRawMatched,
    )
  val `continue`: alpaca.internal.lexer.DefinedToken["continue", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["continue", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["continue"]("continue", "token34", "continue"),
      (`_$3₃₅`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₃`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₃`.lastRawMatched,
    )
  val `return`: alpaca.internal.lexer.DefinedToken["return", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["return", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["return"]("return", "token35", "return"),
      (`_$3₃₆`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₄`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₄`.lastRawMatched,
    )
  val eye: alpaca.internal.lexer.DefinedToken["eye", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["eye", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["eye"]("eye", "token36", "eye"),
      (`_$3₃₇`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₅`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₅`.lastRawMatched,
    )
  val zeros: alpaca.internal.lexer.DefinedToken["zeros", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["zeros", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["zeros"]("zeros", "token37", "zeros"),
      (`_$3₃₈`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₆`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₆`.lastRawMatched,
    )
  val ones: alpaca.internal.lexer.DefinedToken["ones", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["ones", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["ones"]("ones", "token38", "ones"),
      (`_$3₃₉`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₇`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₇`.lastRawMatched,
    )
  val print: alpaca.internal.lexer.DefinedToken["print", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["print", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["print"]("print", "token39", "print"),
      (`_$3₄₀`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₈`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₈`.lastRawMatched,
    )
  val ID: alpaca.internal.lexer.DefinedToken["ID", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["ID", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["ID"]("ID", "token40", "[a-zA-Z_][a-zA-Z0-9_]*"),
      (`_$3₄₁`: alpaca.LexerCtx.Default) => (),
      (`_$2₂₉`: alpaca.LexerCtx.Default @annotation.unchecked.uncheckedVariance) => `_$2₂₉`.lastRawMatched,
    )
  val FLOAT: alpaca.internal.lexer.DefinedToken["FLOAT", alpaca.LexerCtx.Default, Double] =
    alpaca.internal.lexer.DefinedToken.apply["FLOAT", alpaca.LexerCtx.Default, Double](
      alpaca.internal.lexer.TokenInfo.apply["FLOAT"]("FLOAT", "token41", "(\\d+(\\.\\d*)|\\.\\d+)([eE][+-]?\\d+)?"),
      (`_$3₄₂`: alpaca.LexerCtx.Default) => (),
      ($arg0: alpaca.LexerCtx.Default) => Predef.augmentString($arg0.lastRawMatched).toDouble,
    )
  val INTNUM: alpaca.internal.lexer.DefinedToken["INTNUM", alpaca.LexerCtx.Default, Int] =
    alpaca.internal.lexer.DefinedToken.apply["INTNUM", alpaca.LexerCtx.Default, Int](
      alpaca.internal.lexer.TokenInfo.apply["INTNUM"]("INTNUM", "token42", "[0-9]+"),
      (`_$3₄₃`: alpaca.LexerCtx.Default) => (),
      (`$arg0₂`: alpaca.LexerCtx.Default) => Predef.augmentString(`$arg0₂`.lastRawMatched).toInt,
    )
  val STRING: alpaca.internal.lexer.DefinedToken["STRING", alpaca.LexerCtx.Default, java.lang.String] =
    alpaca.internal.lexer.DefinedToken.apply["STRING", alpaca.LexerCtx.Default, java.lang.String](
      alpaca.internal.lexer.TokenInfo.apply["STRING"]("STRING", "token43", "\"[^\"]*\""),
      (`_$3₄₄`: alpaca.LexerCtx.Default) => (),
      (`$arg0₃`: alpaca.LexerCtx.Default) => `$arg0₃`.lastRawMatched.substring(1, `$arg0₃`.lastRawMatched.length().-(1)),
    )
  override val tokens: collection.immutable.List[alpaca.internal.lexer.Token[
    ? >: Nothing <: Any,
    alpaca.LexerCtx.Default,
    ? >: Nothing <: Any,
  ]] = List.apply(
    alpaca.internal.lexer.IgnoredToken.apply[" ", alpaca.LexerCtx.Default](
      alpaca.internal.lexer.TokenInfo.apply[" "](" ", "token44", " "),
      (`_$3₄₅`: alpaca.LexerCtx.Default) => (),
    ),
    alpaca.internal.lexer.IgnoredToken.apply["\t", alpaca.LexerCtx.Default](
      alpaca.internal.lexer.TokenInfo.apply["\t"]("\t", "token45", "\t"),
      (`_$3₄₆`: alpaca.LexerCtx.Default) => (),
    ),
    alpaca.internal.lexer.IgnoredToken.apply["\\#.*", alpaca.LexerCtx.Default](
      alpaca.internal.lexer.TokenInfo.apply["\\#.*"]("\\#.*", "token46", "\\#.*"),
      (`_$3₄₇`: alpaca.LexerCtx.Default) => (),
    ),
    alpaca.internal.lexer.IgnoredToken.apply["\n+", alpaca.LexerCtx.Default](
      alpaca.internal.lexer.TokenInfo.apply["\n+"]("\n+", "token47", "\n+"),
      (`$arg0₄`: alpaca.LexerCtx.Default) => {
        `$arg0₄`.line = `$arg0₄`.line.+(
          Predef.augmentString(`$arg0₄`.lastRawMatched).count((`_$1₁₃`: Char) => `_$1₁₃`.==('\n')) - 1,
        )
        ()
      },
    ),
    this.ADDASSIGN,
    this.SUBASSIGN,
    this.MULASSIGN,
    this.DIVASSIGN,
    this.NOT_EQUAL,
    this.LESS_EQUAL,
    this.GREATER_EQUAL,
    this.EQUAL,
    this.<,
    this.>,
    this.`=`,
    this.`\\+`,
    this.-,
    this.`\\*`,
    this.`/`,
    this.`\\(`,
    this.`\\)`,
    this.`\\[`,
    this.`\\]`,
    this.`\\{`,
    this.`\\}`,
    this.`:`,
    this.`\'`,
    this.`,`,
    this.`;`,
    this.DOTADD,
    this.DOTSUB,
    this.DOTMUL,
    this.DOTDIV,
    this.`if`,
    this.`else`,
    this.`for`,
    this.`while`,
    this.`break`,
    this.`continue`,
    this.`return`,
    this.eye,
    this.zeros,
    this.ones,
    this.print,
    this.ID,
    this.FLOAT,
    this.INTNUM,
    this.STRING,
  )
}
