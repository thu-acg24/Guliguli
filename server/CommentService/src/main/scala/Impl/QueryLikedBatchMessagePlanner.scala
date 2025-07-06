package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryLikedBatchMessagePlanner(
    token: String,
    commentIDs: List[Int],
    override val planContext: PlanContext
) extends Planner[List[Boolean]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Boolean]] = {
    for {
      // Step 1: Check if the commentIDs exists
      _ <- IO(logger.info(s"[QueryLikedBatchMessage] 获取userID"))
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"[QueryLikedBatchMessage] 获取点赞状态"))
      result <- queryLikeStatus(userID, commentIDs)
    } yield result
  }

  private def queryLikeStatus(userID: Int, commentIDs: List[Int])(using PlanContext): IO[List[Boolean]] = {
    if (commentIDs.isEmpty) return IO.pure(List.empty)

    // 构建SQL查询
    val placeholders = commentIDs.map(_ => "?").mkString(",")
    val sqlQuery =
      s"""
        |SELECT comment_id
        |FROM $schemaName.like_comment_record_table
        |WHERE user_id = ?
          |AND comment_id IN ($placeholders)
      """.stripMargin

    // 准备参数
    val parameters =
      SqlParameter("Int", userID.toString) ::
        commentIDs.map(id => SqlParameter("Int", id.toString))

    readDBRows(sqlQuery, parameters).map { jsonList =>
      val likedCommentIDs = jsonList.map(json => decodeField[Int](json, "comment_id")).toSet
      commentIDs.map(likedCommentIDs.contains)
    }
  }
}