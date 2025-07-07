package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.HistoryService.HistoryRecord
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * QueryHistoryMessagePlanner: 根据用户Token校验后，查询用户的观看历史记录。
 * 返回按照 (lastTime, lastID) 开始降序排序至多 fetchLimit 条记录
 */

case class QueryHistoryMessagePlanner(
                                       token: String,
                                       lastTime: DateTime,
                                       lastID: Int,
                                       fetchLimit: Int = 10,
                                       override val planContext: PlanContext
                                     ) extends Planner[List[HistoryRecord]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[HistoryRecord]] = {
    if (fetchLimit > 100) return IO.raiseError(InvalidInputException("至多查询100条记录"))
    for {
      // Step 1: 验证用户Token
      _ <- IO(logger.info(s"开始验证Token: $token"))
      userID <- validateToken(token)
      // Step 2: 查询并分页提取用户的历史记录
      _ <- IO(logger.info(s"Token验证结果: $userID"))
      historyRecords <- queryHistoryByToken(userID)
    } yield historyRecords
  }

  /**
   * Step 1: 验证用户Token是否合法。
   * 调用公共方法GetUIDByTokenMessage验证Token是否有效。
   */
  private def validateToken(token: String)(using PlanContext): IO[Int] = {
    IO(logger.info(s"调用API获取用户ID，参数Token: $token"))>>
    GetUIDByTokenMessage(token).send
  }

  /**
   * Step 2: 根据验证结果查询用户的观看历史记录。
   * 如果Token无效，直接返回None；否则查询用户历史记录表。
   */
  private def queryHistoryByToken(userID: Int)(using PlanContext): IO[List[HistoryRecord]] = {

        IO(logger.info(s"用户合法，开始查询历史记录，用户ID: $userID")) >> 
          queryUserHistory(userID)
      
  }

  /**
   * 使用用户ID和范围（rangeL, rangeR）从数据库查询用户的历史记录。
   * 按照timestamp从新到旧排序，分页读取。
   */
  private def queryUserHistory(userID: Int)(using PlanContext): IO[List[HistoryRecord]] = {
    IO(logger.info(s"开始创建查询用户历史记录的SQL，用户ID: $userID")) >>
      IO {
        val sql =
          s"""
            SELECT history_id, user_id, video_id, view_time
            FROM $schemaName.history_record_table
            WHERE user_id = ? AND (view_time < ? OR (view_time = ? AND history_id < ?))
            ORDER BY view_time DESC, history_id DESC
            LIMIT ?;
          """
        logger.info(s"SQL为: $sql")

        // SQL参数列表
        val parameters = List(
          SqlParameter("Int", userID.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("Int", lastID.toString),
          SqlParameter("Int", fetchLimit.toString),
        )

        (sql, parameters)
      }.flatMap { case (sql, parameters) =>
        IO(logger.info("开始执行SQL查询用户历史记录")) *>
        readDBRows(sql, parameters).flatMap { rows =>
          IO(logger.info(s"查询成功，记录数量: ${rows.size}"))*>
          IO.pure(
            rows.map { row =>
              HistoryRecord(
                historyID = decodeField[Int](row, "history_id"),
                userID = decodeField[Int](row, "user_id"),
                videoID = decodeField[Int](row, "video_id"),
                timestamp = decodeField[DateTime](row, "view_time")
              )
            }
          )
        }
      }
  }
}