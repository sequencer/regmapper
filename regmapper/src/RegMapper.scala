package org.chipsalliance.regmapper

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleGenerator, SerializableModuleParameter}
import chisel3.util._
object RegMapperParameter {
  implicit def rwP: upickle.default.ReadWriter[RegMapperParameter] = upickle.default.macroRW
}

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
  supportMask:     Boolean,
  regMap:          Seq[(Int, Seq[RegFieldGenerator[_, _]])]) extends SerializableModuleParameter {
  assert(indexBits > 0 && dataBits > 0, "width must be positive")
  assert(isPow2(dataGranularity), "dataGranularity must be a power of 2")
  assert(isPow2(dataBits), "dataGranularity must be a power of 2")
  assert(dataGranularity <= dataBits, "dataGranularity must be <= dataBits")
  assert(
    supportRead || supportWrite,
    "must support at least one of read or write"
  )

  /** the number of bits in the mask field */
  private[regmapper] def maskBits: Option[Int] =
    if (supportMask) Some(dataBits / dataGranularity) else None
}

class FieldIO(val dataWidth: Int) extends Bundle {
  val read: Bool = Input(Bool())
  val write: Bool = Input(Bool())
  val wdata: UInt = Input(UInt(dataWidth.W))
  val rdata: UInt = Output(UInt(dataWidth.W))
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
class RegMapper(val parameter: RegMapperParameter)
  extends Module
  with SerializableModule[RegMapperParameter] {
  val busIO = IO(new RegMapperIO(parameter))
  val fieldIO = parameter.regMap.flatMap(_._2).map(rf => IO(new FieldIO(rf.parameter.asInstanceOf[RegFieldParameter].width)).suggestName(s"field_${rf.parameter.asInstanceOf[RegFieldParameter].name}"))

  val sels = parameter.regMap.map(r => busIO.index === r._1.U)
  val wens = VecInit(sels.map(_ && busIO.write))
  val rens = VecInit(sels.map(_ && busIO.read))
  val osels = RegNext(rens, VecInit(List.tabulate(rens.length)(_ => false.B)))

  val startEndReg = parameter.regMap.map(r => r._2.foldLeft((0, Seq[(Int, Int)]())) { case ((start, lastResult), rf) =>
    val end = start + rf.parameter.asInstanceOf[RegFieldParameter].width - 1
    (end + 1, lastResult :+ (start, end))
  }._2)

  fieldIO.zip(startEndReg.flatten).foreach { case (rio, (start, end)) =>
    rio.wdata := busIO.wdata(end, start)
  }

  fieldIO.zip(parameter.regMap.zipWithIndex.flatMap(r => r._1._2.map(_ => r._2))).foreach { case (rio, i) =>
    rio.read := rens(i)
    rio.write := wens(i)
  }

  val idRegField = parameter.regMap.zipWithIndex.flatMap { case ((_, r), i) => r.map(_ => i)}

  val rdatas = fieldIO.zip(idRegField).groupBy(_._2).toSeq.sortBy(_._1).map(_._2).map(r => Cat(r.map(_._1.rdata)))

  busIO.rdata := rdatas.zip(osels).map(r => Mux(r._2, r._1, 0.U)).reduce(_ | _)
}

object main extends App {
  val j = upickle.default.write(
    SerializableModuleGenerator(
      classOf[RegMapper],
      RegMapperParameter(
        indexBits = 8, dataBits = 32, dataGranularity = 8, supportRead = true, supportWrite = true, supportMask = false,
        regMap = Seq(
          0 -> Seq(RegField(3), RegField(2), RegField(1)),
          1 -> Seq(RegField(6), RegField(4), RegField(2))
        )
      )
    )
  )

  println(j)

//  println(
    (new chisel3.stage.ChiselStage).emitFirrtl(upickle.default.read[SerializableModuleGenerator[RegMapper, RegMapperParameter]](
      ujson.read(j)
    ).module())
//  )
}
