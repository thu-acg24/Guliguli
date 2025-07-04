package Objects

import io.circe.generic.auto.*
import io.circe.syntax.*

case class AvatarPayload(id: Int, token: String, file_name: String, task: String = "avatar")
