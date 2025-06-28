package APIs.DanmakuService

import Common.API.API
import Global.ServiceCenter.DanmakuServiceCode

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID


/**
 * PublishDanmakuMessage
 * desc: 用于发布弹幕功能点
 * @param token: String (用户凭据，用于验证身份是否合法)
 * @param videoID: Int (目标视频的唯一标识符)
 * @param timeInVideo: Float (弹幕出现的时间点（单位：秒）)
 * @param danmakuContent: String (弹幕的文字内容)
 * @param danmakuColor: String (弹幕的颜色值（例如：#FFFFFF）)
 * @return result: String (返回操作结果，成功返回None，失败返回具体错误信息)
 */

case class PublishDanmakuMessage(
  token: String,
  videoID: Int,
  timeInVideo: Float,
  danmakuContent: String,
  danmakuColor: String
) extends API[Option[String]](DanmakuServiceCode)



case object PublishDanmakuMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

