package Objects.VideoService


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

@JsonSerialize(`using` = classOf[VideoStatusSerializer])
@JsonDeserialize(`using` = classOf[VideoStatusDeserializer])
enum VideoStatus(val desc: String):

  override def toString: String = this.desc

  case Pending extends VideoStatus("Pending") // 待审核
  case Approved extends VideoStatus("Approved") // 审核通过
  case Rejected extends VideoStatus("Rejected") // 审核未通过
  case Uploading extends VideoStatus("Uploading") // 上传中
  case Private extends VideoStatus("Private") // 公众不可见

object VideoStatus:
  given encode: Encoder[VideoStatus] = Encoder.encodeString.contramap[VideoStatus](toString)

  given decode: Decoder[VideoStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):VideoStatus  = s match
    case "Pending" => Pending
    case "Approved" => Approved
    case "Rejected" => Rejected
    case "Uploading" => Uploading
    case "Private" => Private
    case _ => throw Exception(s"Unknown VideoStatus: $s")

  def fromStringEither(s: String):Either[String, VideoStatus]  = s match
    case "Pending" => Right(Pending)
    case "Approved" => Right(Approved)
    case "Rejected" => Right(Rejected)
    case "Uploading" => Right(Uploading)
    case "Private" => Right(Private)
    case _ => Left(s"Unknown VideoStatus: $s")

  def toString(t: VideoStatus): String = t match
    case Pending => "Pending"
    case Approved => "Approved"
    case Rejected => "Rejected"
    case Uploading => "Uploading"
    case Private => "Private"

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
