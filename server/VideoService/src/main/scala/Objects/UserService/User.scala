package Objects.UserService


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
import Objects.UserService.UserRole

/**
 * User
 * desc: 用户数据结构，用于存储用户的基本信息和状态
 * @param userID: Int (用户的唯一ID)
 * @param username: String (用户名)
 * @param email: String (用户的邮箱地址)
 * @param passwordHash: String (密码的哈希值)
 * @param avatarPath: String (用户头像的存储路径)
 * @param userRole: UserRole:1084 (用户的角色)
 * @param videoCount: Int (用户上传的视频数量)
 * @param isBanned: Boolean (用户是否被禁用)
 */

case class User(
  userID: Int,
  username: String,
  email: String,
  passwordHash: String,
  avatarPath: String,
  userRole: UserRole,
  videoCount: Int,
  isBanned: Boolean
){

  //process class code 预留标志位，不要删除


}


case object User{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[User] = deriveEncoder
  private val circeDecoder: Decoder[User] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[User] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[User] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[User]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given userEncoder: Encoder[User] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given userDecoder: Decoder[User] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

