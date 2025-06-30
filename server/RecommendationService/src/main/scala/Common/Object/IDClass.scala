package Common.Object


import Common.Serialize.CustomColumnTypes.decodeIDClassBase
import Common.Serialize.CustomColumnTypes.encodeIDClassBase
import io.circe.Decoder
import io.circe.Encoder

case class IDClass(v: Long)

object IDClass {

  implicit val encodeIDClass: Encoder[IDClass] = encodeIDClassBase[IDClass]
  implicit val decodeIDClass: Decoder[IDClass] = decodeIDClassBase[IDClass](using IDClass.apply)
}