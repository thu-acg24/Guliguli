package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class GetUIDByTokenMessagePlanner(
                                        token: String,
                                        override val planContext: PlanContext
                                      ) extends Planner[Int] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info(s"[Step 1] 开始解析Token: $token"))
      userID <- validateToken(token)
      _ <- IO(logger.info(s"[Step 2] Token有效，对应的userID为: $userID"))
    } yield userID
  }
}