package org.chipsalliance.regmapper

import chisel3._

abstract class RegField extends Module {
  val width:         Int
  val readFunction:  Bool => UInt
  val writeFunction: (Bool, UInt) => Unit

  val read:  Bool = IO(Input(Bool()))
  val write: Bool = IO(Input(Bool()))
  val wdata: UInt = IO(Input(UInt(width.W)))
  val rdata: UInt = IO(Output(UInt(width.W)))
  rdata := readFunction(read)
  writeFunction(write, wdata)
}

class SimpleRegField(val width: Int) extends RegField {
  val readFunction: Bool => UInt = { _ => reg }
  val writeFunction: (Bool, UInt) => Unit = { case (we, data) => reg := Mux(we, data, reg) }
  val reg = RegInit(0.U(width.W))
}

class ConstantRegField(val width: Int, constant: BigInt) extends RegField {
  val readFunction: Bool => UInt = { _ => constant.U(width.W) }
  val writeFunction: (Bool, UInt) => Unit = { case (_, _) => }
}

class ZeroRegField(override val width: Int) extends ConstantRegField(width, 0)
