package Objects.UserService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[UserRoleSerializer])
@JsonDeserialize(`using` = classOf[UserRoleDeserializer])
enum UserRole(val desc: String):

  override def toString: String = this.desc

  case Admin extends UserRole("管理员") // 管理员
  case Auditor extends UserRole("审核员") // 审核员
  case Normal extends UserRole("普通用户") // 普通用户


object UserRole:
  given encode: Encoder[UserRole] = Encoder.encodeString.contramap[UserRole](toString)

  given decode: Decoder[UserRole] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):UserRole  = s match
    case "管理员" => Admin
    case "审核员" => Auditor
    case "普通用户" => Normal
    case _ => throw Exception(s"Unknown UserRole: $s")

  def fromStringEither(s: String):Either[String, UserRole]  = s match
    case "管理员" => Right(Admin)
    case "审核员" => Right(Auditor)
    case "普通用户" => Right(Normal)
    case _ => Left(s"Unknown UserRole: $s")

  def toString(t: UserRole): String = t match
    case Admin => "管理员"
    case Auditor => "审核员"
    case Normal => "普通用户"


// Jackson 序列化器
class UserRoleSerializer extends JsonSerializer[UserRole] {
  override def serialize(value: UserRole, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(UserRole.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class UserRoleDeserializer extends JsonDeserializer[UserRole] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): UserRole = {
    UserRole.fromString(p.getText)
  }
}

