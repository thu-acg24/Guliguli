package Objects

import io.circe.generic.auto.*
import io.circe.syntax.*

case class CoverPayload(id: Int, token: String, file_name: String, task: String = "cover")
