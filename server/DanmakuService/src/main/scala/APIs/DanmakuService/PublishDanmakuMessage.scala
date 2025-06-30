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
 * PublishDanmakuMessage
 * desc: 用于发布弹幕功能点
 * @param token: String (用户凭据，用于验证身份是否合法)
 * @param videoID: Int (目标视频的唯一标识符)
 * @param timeInVideo: Float (弹幕出现的时间点（单位：秒）)
 * @param danmakuContent: String (弹幕的文字内容)
 * @param danmakuColor: String (弹幕的颜色值（例如：#FFFFFF）)
 */

case class PublishDanmakuMessage(
  token: String,
  videoID: Int,
  timeInVideo: Float,
  danmakuContent: String,
  danmakuColor: String
) extends API[Unit](DanmakuServiceCode)

case object PublishDanmakuMessage{

  private val circeEncoder: Encoder[PublishDanmakuMessage] = deriveEncoder
  private val circeDecoder: Decoder[PublishDanmakuMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[PublishDanmakuMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[PublishDanmakuMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[PublishDanmakuMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given publishDanmakuMessageEncoder: Encoder[PublishDanmakuMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given publishDanmakuMessageDecoder: Decoder[PublishDanmakuMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
