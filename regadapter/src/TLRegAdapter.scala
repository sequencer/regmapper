package org.chipsalliance.regadapter

import chisel3._
import chisel3.util._
import org.chipsalliance.regmapper._
import tilelink._

class TLRegAdapter(tilelinkParameter: TLBundleParameter, regParameter: RegMapperParameter) extends Module {
  val io = IO(new Bundle {
    val tl = Flipped(tilelinkParameter.bundle())
    val reg = new RegMapperIO(regParameter)
  })

  val a = io.tl.a
  val d = io.tl.d
  val reg = io.reg

  a.ready := true.B

  reg.read := a.fire && (a.bits.opcode === Message.Get)
  reg.write := a.fire && (a.bits.opcode === Message.PutFullData || a.bits.opcode === Message.PutPartialData)
  reg.index := a.bits.address
  reg.wdata := a.bits.data
  reg.mask.foreach(_ := a.bits.mask)

  val rdataValidHolder = Reg(Bool())
  rdataValidHolder := Mux(d.fire, false.B, reg.read)
  d.valid := (a.fire || rdataValidHolder)

  val rdataHolder = RegEnable(reg.rdata, 0.U(regParameter.dataBits.W), reg.read)
  d.bits.data := Mux(a.fire, reg.rdata, rdataHolder)
}
