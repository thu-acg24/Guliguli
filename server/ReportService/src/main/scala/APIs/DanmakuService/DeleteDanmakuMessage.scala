package APIs.DanmakuService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.DanmakuServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.parser.*
import io.circe.syntax.*
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Try

/**
 * DeleteDanmakuMessage
 * desc: 用于删除弹幕功能点
 * @param token: String (用户的令牌，用于校验用户身份)
 * @param danmakuID: Int (弹幕的唯一标识ID)
 */

case class DeleteDanmakuMessage(
  token: String,
  danmakuID: Int
) extends API[Unit](DanmakuServiceCode)

case object DeleteDanmakuMessage{

  private val circeEncoder: Encoder[DeleteDanmakuMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteDanmakuMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteDanmakuMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteDanmakuMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteDanmakuMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteDanmakuMessageEncoder: Encoder[DeleteDanmakuMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteDanmakuMessageDecoder: Decoder[DeleteDanmakuMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
