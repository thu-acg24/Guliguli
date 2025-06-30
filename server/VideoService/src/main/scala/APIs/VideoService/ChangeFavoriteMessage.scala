package APIs.VideoService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
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
 * ChangeFavoriteMessage
 * desc: 增或删用户收藏记录。
 * @param token: String (用户身份认证Token)
 * @param videoID: Int (视频唯一标识符)
 * @param isFav: Boolean (表示是否收藏(true表示新增收藏，false表示取消收藏))
 */

case class ChangeFavoriteMessage(
  token: String,
  videoID: Int,
  isFav: Boolean
) extends API[Unit](VideoServiceCode)

case object ChangeFavoriteMessage{

  private val circeEncoder: Encoder[ChangeFavoriteMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeFavoriteMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeFavoriteMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeFavoriteMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeFavoriteMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeFavoriteMessageEncoder: Encoder[ChangeFavoriteMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeFavoriteMessageDecoder: Decoder[ChangeFavoriteMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
