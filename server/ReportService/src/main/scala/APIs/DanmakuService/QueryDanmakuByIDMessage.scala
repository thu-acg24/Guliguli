package APIs.DanmakuService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.DanmakuServiceCode
import Objects.DanmakuService.Danmaku
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
 * QueryDanmakuByIDMessage
 * desc: 用于查询弹幕信息功能点
 * @param danmakuID: Int (弹幕的唯一标识符，用于获取具体的弹幕信息)
 * @return danmaku: Danmaku (查询返回的弹幕信息，包括弹幕内容、所属视频、时间点、颜色及作者等)
 */

case class QueryDanmakuByIDMessage(
  danmakuID: Int
) extends API[Danmaku](DanmakuServiceCode)

case object QueryDanmakuByIDMessage{

  private val circeEncoder: Encoder[QueryDanmakuByIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryDanmakuByIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryDanmakuByIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryDanmakuByIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryDanmakuByIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryDanmakuByIDMessageEncoder: Encoder[QueryDanmakuByIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryDanmakuByIDMessageDecoder: Decoder[QueryDanmakuByIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
