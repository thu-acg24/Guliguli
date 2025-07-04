package Objects

import io.circe.syntax._
import io.circe.generic.auto._

case class VideoPayload(id: Int, token: String, file_name: String)
