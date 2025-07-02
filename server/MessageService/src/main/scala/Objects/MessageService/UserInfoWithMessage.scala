package Objects.MessageService

import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime
import Objects.UserService.UserInfo
import java.util.UUID
import scala.util.Try

/**
 * UserInfoWithMessage
 * desc: 消息实体，包含发送方、接收方等基本信息
 * @param userInfo: UserInfo (用户信息)
 * @param unreadCount: Int (未读的消息的数量)
 * @param timestamp: DateTime (最后一条消息发送的时间戳)
 * @param content: String (最后一条消息的内容)
 */

case class UserInfoWithMessage(
  userInfo: UserInfo,
  unreadCount: Int,
  timestamp: DateTime,
  content: String
){

  //process class code 预留标志位，不要删除

}

case object UserInfoWithMessage{

  private val circeEncoder: Encoder[UserInfoWithMessage] = deriveEncoder
  private val circeDecoder: Decoder[UserInfoWithMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UserInfoWithMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UserInfoWithMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UserInfoWithMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }

  // Circe + Jackson 兜底的 Encoder
  given messageEncoder: Encoder[UserInfoWithMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given messageDecoder: Decoder[UserInfoWithMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
