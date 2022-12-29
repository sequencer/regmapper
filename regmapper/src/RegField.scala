package org.chipsalliance.regmapper

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleGenerator, SerializableModuleParameter}

case class RegFieldGenerator[
  P <: RegFieldParameter,
  M <: RegField[P]](
                     override val generator: Class[M],
                     override val parameter: P
                   )
  extends SerializableModuleGenerator[P, M](generator, parameter)

trait RegFieldParameter extends SerializableModuleParameter {
  val width: Int
  val name：String
}

abstract class RegField[T <: RegFieldParameter] extends Module with SerializableModule[T] {
  val parameter: T
  val width: Int
  val readFunction: Bool => UInt
  val writeFunction: (Bool, UInt) => Unit

  val read: Bool = IO(Input(Bool()))
  val write: Bool = IO(Input(Bool()))
  val wdata: UInt = IO(Input(UInt(width.W)))
  val rdata: UInt = IO(Output(UInt(width.W)))
  rdata := readFunction(read)
  writeFunction(write, wdata)
}