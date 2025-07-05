package Objects.UserService


import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.circe.Decoder
import io.circe.Encoder

@JsonSerialize(`using` = classOf[UserRoleSerializer])
@JsonDeserialize(`using` = classOf[UserRoleDeserializer])
enum UserRole(val desc: String):

  override def toString: String = this.desc

  case Admin extends UserRole("Admin") // 管理员
  case Auditor extends UserRole("Auditor") // 审核员
  case Normal extends UserRole("Normal") // 普通用户

object UserRole:
  given encode: Encoder[UserRole] = Encoder.encodeString.contramap[UserRole](toString)

  given decode: Decoder[UserRole] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):UserRole  = s match
    case "Admin" => Admin
    case "Auditor" => Auditor
    case "Normal" => Normal
    case _ => throw Exception(s"Unknown UserRole: $s")

  def fromStringEither(s: String):Either[String, UserRole]  = s match
    case "Admin" => Right(Admin)
    case "Auditor" => Right(Auditor)
    case "Normal" => Right(Normal)
    case _ => Left(s"Unknown UserRole: $s")

  def toString(t: UserRole): String = t match
    case Admin => "Admin"
    case Auditor => "Auditor"
    case Normal => "Normal"

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
