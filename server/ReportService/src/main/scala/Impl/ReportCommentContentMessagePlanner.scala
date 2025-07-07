package Impl


import APIs.CommentService.QueryCommentByIDMessage
import Common.APIException.InvalidInputException
import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ReportCommentContentMessagePlanner(
                                               token: String,
                                               commentID: Int,
                                               reason: String,
                                               override val planContext: PlanContext
                                             ) extends Planner[Unit] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验 token 是否有效并获取 userID
      _ <- IO(logger.info(s"Validating token: $token"))
      userID <- GetUIDByTokenMessage(token).send
      // Step 2: 检测评论是否存在
      _ <- QueryCommentByIDMessage(commentID).send
      // Step 3: 检查重复举报
      _ <- IO(logger.info(s"Checking for duplicate pending reports for commentID: $commentID and userID: $userID"))
      alreadyExists <- checkDuplicateReport(commentID, userID)
      _ <- if alreadyExists then IO.raiseError(InvalidInputException("已经举报过该评论")) else IO.unit
      // Step 4: 插入举报记录
      _ <- IO(logger.info(s"Inserting new report for commentID: $commentID, userID: $userID, reason: $reason"))
      _ <- insertReportRecord(userID, commentID, reason)
    } yield ()
  }

  private def checkDuplicateReport(commentID: Int, userID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT 1 FROM $schemaName.report_comment_table
         WHERE comment_id = ? AND reporter_id = ? AND status = 'Pending'
       """
    readDBJsonOptional(sql, List(
      SqlParameter("Int", commentID.toString),
      SqlParameter("Int", userID.toString)
    )).map(_.isDefined)
  }

  private def insertReportRecord(userID: Int, commentID: Int, reason: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         INSERT INTO $schemaName.report_comment_table
         (comment_id, reporter_id, reason, status, timestamp)
         VALUES (?, ?, ?, 'Pending', ?)
       """
    IO(DateTime.now().getMillis.toString).flatMap { timestamp =>
      writeDB(sql, List(
        SqlParameter("Int", commentID.toString),
        SqlParameter("Int", userID.toString),
        SqlParameter("String", reason),
        SqlParameter("DateTime", timestamp)
      ))
    }
  }
}