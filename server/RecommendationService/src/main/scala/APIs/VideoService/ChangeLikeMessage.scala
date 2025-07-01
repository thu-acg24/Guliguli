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
 * ChangeLikeMessage
 * desc: 增或删用户点赞记录。
 * @param token: String (用户的校验Token，用于确认登录身份)
 * @param videoID: Int (视频的唯一标识符)
 * @param isLike: Boolean (点赞操作标志，true表示点赞，false表示取消点赞)
 */

case class ChangeLikeMessage(
  token: String,
  videoID: Int,
  isLike: Boolean
) extends API[Unit](VideoServiceCode)

case object ChangeLikeMessage{

  private val circeEncoder: Encoder[ChangeLikeMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeLikeMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeLikeMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeLikeMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeLikeMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeLikeMessageEncoder: Encoder[ChangeLikeMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeLikeMessageDecoder: Decoder[ChangeLikeMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
