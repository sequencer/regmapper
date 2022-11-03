package org.chipsalliance.regmapper

import chisel3._
import chisel3.util.isPow2
import upickle.default.{macroRW, ReadWriter => RW}

/** Parameters for a register mapper
  *
  * @param indexBits
  * the number of bits in the index field
  * @param dataBits
  * the number of bits in the data field
  * @param dataGranularity
  * the number of bits in the data field that can be written atomically
  * @param supportRead
  * whether the device supports read operations
  * @param supportWrite
  * whether the device supports write operations
  * @param supportMask
  * whether the device supports write masks
  */
case class RegMapperParameter(
  indexBits:       Int,
  dataBits:        Int,
  dataGranularity: Int,
  supportRead:     Boolean,
  supportWrite:    Boolean,
  supportMask:     Boolean) {
  assert(indexBits > 0 && dataBits > 0, "width must be positive")
  assert(isPow2(dataGranularity), "dataGranularity must be a power of 2")
  assert(isPow2(dataBits), "dataGranularity must be a power of 2")
  assert(dataGranularity <= dataBits, "dataGranularity must be <= dataBits")
  assert(
    !supportRead && !supportWrite,
    "must support at least one of read or write"
  )

  /** the number of bits in the mask field */
  private[regmapper] def maskBits: Option[Int] =
    if (supportMask) Some(dataBits / dataGranularity) else None
}

/** Input to a register mapper
  *
  * @param parameter
  * the parameters for the register mapper
  */
class RegMapperIO(val parameter: RegMapperParameter) extends Bundle {
  val read:  Bool = Input(Bool())
  val write: Bool = Input(Bool())
  val index: UInt = Input(UInt(parameter.indexBits.W))
  val wdata: UInt = Input(UInt(parameter.dataBits.W))
  val mask:  Option[UInt] = parameter.maskBits.flatMap(m => Some(Input(UInt(m.W))))
  val rdata: UInt = Output(UInt(parameter.dataBits.W))
}

/** A bus agnostic register interface to a register-based device
  *
  * @param parameter
  * the parameter of the register mapper
  */
class RegMapper(parameter: RegMapperParameter) extends Module {
  val io = IO(new RegMapperIO(parameter))
}
