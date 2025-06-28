package Objects.VideoService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[VideoStatusSerializer])
@JsonDeserialize(`using` = classOf[VideoStatusDeserializer])
enum VideoStatus(val desc: String):

  override def toString: String = this.desc

  case Pending extends VideoStatus("待审核") // 待审核
  case Approved extends VideoStatus("审核通过") // 审核通过
  case Rejected extends VideoStatus("审核未通过") // 审核未通过


object VideoStatus:
  given encode: Encoder[VideoStatus] = Encoder.encodeString.contramap[VideoStatus](toString)

  given decode: Decoder[VideoStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):VideoStatus  = s match
    case "待审核" => Pending
    case "审核通过" => Approved
    case "审核未通过" => Rejected
    case _ => throw Exception(s"Unknown VideoStatus: $s")

  def fromStringEither(s: String):Either[String, VideoStatus]  = s match
    case "待审核" => Right(Pending)
    case "审核通过" => Right(Approved)
    case "审核未通过" => Right(Rejected)
    case _ => Left(s"Unknown VideoStatus: $s")

  def toString(t: VideoStatus): String = t match
    case Pending => "待审核"
    case Approved => "审核通过"
    case Rejected => "审核未通过"


// Jackson 序列化器
class VideoStatusSerializer extends JsonSerializer[VideoStatus] {
  override def serialize(value: VideoStatus, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(VideoStatus.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class VideoStatusDeserializer extends JsonDeserializer[VideoStatus] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): VideoStatus = {
    VideoStatus.fromString(p.getText)
  }
}

