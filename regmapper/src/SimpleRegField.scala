package org.chipsalliance.regmapper

import chisel3._
import chisel3.util.Valid

case class SimpleRegFieldGenerator(p: SimpleRegFieldParameter)
  extends RegFieldGenerator[SimpleRegFieldParameter, SimpleRegField](classOf[SimpleRegField], p)

object SimpleRegFieldParameter {
  implicit def rwP: upickle.default.ReadWriter[SimpleRegFieldParameter] = upickle.default.macroRW
}

case class SimpleRegFieldParameter(
                                    width: Int,
                                    hasRead: Boolean,
                                    hasWrite: Boolean,
                                    customWritePortFirst: Boolean)
  extends RegFieldParameter

class SimpleRegField(val parameter: SimpleRegFieldParameter) extends
  RegField[SimpleRegFieldParameter] {
  val width: Int = parameter.width
  val reg = RegInit(0.U(width.W))

  val regReadPort = if (parameter.hasRead) Some(IO(Output(UInt(width.W)))) else None
  val regWritePort = if (parameter.hasWrite) Some(IO(Input(Valid(UInt(width.W))))) else None

  // TODO: datapath for regWritePort
  val readFunction: Bool => UInt = { _ => reg }
  val writeFunction: (Bool, UInt) => Unit = {
    // TODO: datapath for regWritePort
    case (we, data) => reg := Mux(we, data, reg)
  }
}
