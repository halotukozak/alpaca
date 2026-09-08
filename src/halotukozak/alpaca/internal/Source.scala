package halotukozak.alpaca.internal

import halotukozak.mcodec.MCodec

import scala.quoted.ToExprFactory

case class Source(line: Int, file: String) derives MCodec, ToExprFactory
