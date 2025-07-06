package Impl

import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryCommentCountMessagePlanner(
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[Int] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Int] = {
    val sql =
      s"""
         |SELECT SUM(reply_count)
         |FROM ${schemaName}.comment_table
         |WHERE video_id = ? AND reply_to_id IS NULL;
       """.stripMargin
    readDBInt(sql, List(SqlParameter("Int", videoID.toString)))
  }
}