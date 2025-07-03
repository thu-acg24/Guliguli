package Objects.VideoService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
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
 * UploadPath
 * desc: 视频收藏记录，记录用户收藏视频的相关信息
 * @param coverPath: String (封面上传链接)
 * @param coverToken: String (封面上传会话Token)
 * @param videoPath: String (视频上传链接)
 * @param videoToken: String (视频上传会话Token)
 */

case class UploadPath(
  coverPath: String,
  coverToken: String,
  videoPath: String,
  videoToken: String 
){

  //process class code 预留标志位，不要删除

}

case object UploadPath{

  private val circeEncoder: Encoder[UploadPath] = deriveEncoder
  private val circeDecoder: Decoder[UploadPath] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UploadPath] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UploadPath] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UploadPath]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given uploadPathEncoder: Encoder[UploadPath] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given uploadPathDecoder: Decoder[UploadPath] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
