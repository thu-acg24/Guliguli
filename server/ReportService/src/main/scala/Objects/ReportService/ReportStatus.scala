package Objects.ReportService


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

@JsonSerialize(`using` = classOf[ReportStatusSerializer])
@JsonDeserialize(`using` = classOf[ReportStatusDeserializer])
enum ReportStatus(val desc: String):

  override def toString: String = this.desc

  case Pending extends ReportStatus("待处理") // 待处理
  case Resolved extends ReportStatus("已处理") // 已处理
  case Rejected extends ReportStatus("驳回") // 驳回

object ReportStatus:
  given encode: Encoder[ReportStatus] = Encoder.encodeString.contramap[ReportStatus](toString)

  given decode: Decoder[ReportStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):ReportStatus  = s match
    case "待处理" => Pending
    case "已处理" => Resolved
    case "驳回" => Rejected
    case _ => throw Exception(s"Unknown ReportStatus: $s")

  def fromStringEither(s: String):Either[String, ReportStatus]  = s match
    case "待处理" => Right(Pending)
    case "已处理" => Right(Resolved)
    case "驳回" => Right(Rejected)
    case _ => Left(s"Unknown ReportStatus: $s")

  def toString(t: ReportStatus): String = t match
    case Pending => "待处理"
    case Resolved => "已处理"
    case Rejected => "驳回"

// Jackson 序列化器
class ReportStatusSerializer extends JsonSerializer[ReportStatus] {
  override def serialize(value: ReportStatus, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(ReportStatus.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class ReportStatusDeserializer extends JsonDeserializer[ReportStatus] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ReportStatus = {
    ReportStatus.fromString(p.getText)
  }
}
