package Impl


import Objects.CommentService.Comment
import APIs.CommentService.QueryCommentByIDMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
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
import APIs.UserService.GetUIDByTokenMessage
import io.circe._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ReportCommentContentMessagePlanner(
                                               token: String,
                                               commentID: Int,
                                               reason: String,
                                               override val planContext: PlanContext
                                             ) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验 token 是否有效并获取 userID
      _ <- IO(logger.info(s"Validating token: ${token}"))
      userIDOpt <- GetUIDByTokenMessage(token).send
      userID <- userIDOpt match {
        case Some(uid) => IO.pure(uid)
        case None =>
          IO(logger.info(s"Invalid token: ${token}")) >>
          IO.pure(Some("Invalid Token"))
      }

      // 如果 token 校验失败直接返回
      result <- userIDOpt match {
        case None => IO.pure(Some("Invalid Token")) // 直接返回错误，没有进一步操作
        case Some(userID) =>
          for {
            // Step 2: 校验评论是否存在
            _ <- IO(logger.info(s"Checking if commentID: ${commentID} exists"))
            commentOpt <- validateCommentExists(commentID)
            _ <- logCommentCheckResult(commentOpt)

            // 如果评论不存在直接返回错误
            res <- commentOpt match {
              case None => IO.pure(Some("Comment Not Found or Deleted"))
              case Some(_) =>
                for {
                  // Step 3: 检查重复举报
                  _ <- IO(logger.info(s"Checking for duplicate pending reports for commentID: ${commentID} and userID: ${userID}"))
                  isDuplicate <- checkDuplicateReport(commentID, userID)
                  _ <- IO(logger.info(s"Is duplicate report: ${isDuplicate}"))

                  // 如果存在重复的Pending举报记录直接返回
                  result <- if (isDuplicate) IO.pure(Some("Duplicate Pending Report Found")) else {
                    // Step 4: 插入举报记录
                    _ <- IO(logger.info(s"Inserting new report for commentID: ${commentID}, userID: ${userID}, reason: ${reason}"))
                    insertResult <- insertReportRecord(userID, commentID, reason)
                    _ <- IO(logger.info(s"Insert result: ${insertResult}"))

                    // 返回操作结果
                    IO.pure(if (insertResult) None else Some("Failed to save report record"))
                  }
                } yield result
            }
          } yield res
      }
    } yield result
  }

  private def validateCommentExists(commentID: Int)(using PlanContext): IO[Option[Comment]] = {
    QueryCommentByIDMessage(commentID).send
  }

  private def logCommentCheckResult(commentOpt: Option[Comment])(using PlanContext): IO[Unit] = {
    commentOpt match {
      case Some(comment) => IO(logger.info(s"Comment exists: ${comment}"))
      case None => IO(logger.info(s"Comment not found for commentID: ${commentID}"))
    }
  }

  private def checkDuplicateReport(commentID: Int, userID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT 1 FROM ${schemaName}.report_comment_table
         WHERE comment_id = ? AND reporter_id = ? AND status = 'Pending'
       """
    readDBJsonOptional(sql, List(
      SqlParameter("Int", commentID.toString),
      SqlParameter("Int", userID.toString)
    )).map(_.isDefined)
  }

  private def insertReportRecord(userID: Int, commentID: Int, reason: String)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.report_comment_table
         (comment_id, reporter_id, reason, status, timestamp)
         VALUES (?, ?, ?, 'Pending', ?)
       """
    val timestamp = DateTime.now().getMillis.toString
    writeDB(sql, List(
      SqlParameter("Int", commentID.toString),
      SqlParameter("Int", userID.toString),
      SqlParameter("String", reason),
      SqlParameter("DateTime", timestamp)
    )).map(_ == "Operation(s) done successfully")
  }
}