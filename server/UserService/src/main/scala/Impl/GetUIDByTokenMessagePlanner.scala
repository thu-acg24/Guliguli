package Impl


import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.validateToken
import Common.DBAPI._
import Common.Object.SqlParameter
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName

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