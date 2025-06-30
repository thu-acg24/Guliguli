package Objects.UserService


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
 * FollowRelation
 * desc: 用户之间的关注关系
 * @param followerID: Int (关注者的用户ID)
 * @param followeeID: Int (被关注者的用户ID)
 * @param timestamp: DateTime (关注的时间戳)
 */

case class FollowRelation(
  followerID: Int,
  followeeID: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除

}

case object FollowRelation{

  private val circeEncoder: Encoder[FollowRelation] = deriveEncoder
  private val circeDecoder: Decoder[FollowRelation] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FollowRelation] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FollowRelation] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FollowRelation]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given followRelationEncoder: Encoder[FollowRelation] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given followRelationDecoder: Decoder[FollowRelation] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
