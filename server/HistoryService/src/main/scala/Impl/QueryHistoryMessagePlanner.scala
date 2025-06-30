package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.HistoryService.HistoryRecord
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * QueryHistoryMessagePlanner: 根据用户Token校验后，查询用户的观看历史记录。
 * 返回从新到旧第rangeL条到第rangeR条记录（均包含）。
 */

case class QueryHistoryMessagePlanner(
                                       token: String,
                                       rangeL: Int,
                                       rangeR: Int,
                                       override val planContext: PlanContext
                                     ) extends Planner[Option[List[HistoryRecord]]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[List[HistoryRecord]]] = {
    for {
      // Step 1: 验证用户Token
      _ <- IO(logger.info(s"开始验证Token: ${token}"))
      userIDOption <- validateToken(token)
      
      // Step 2: 查询并分页提取用户的历史记录
      _ <- IO(logger.info(s"Token验证结果: ${userIDOption}"))
      historyRecords <- queryHistoryByToken(userIDOption)
    } yield historyRecords
  }

  /**
   * Step 1: 验证用户Token是否合法。
   * 调用公共方法GetUIDByTokenMessage验证Token是否有效。
   */
  private def validateToken(token: String)(using PlanContext): IO[Option[Int]] = {
    logger.info(s"调用API获取用户ID，参数Token: ${token}")
    GetUIDByTokenMessage(token).send
  }

  /**
   * Step 2: 根据验证结果查询用户的观看历史记录。
   * 如果Token无效，直接返回None；否则查询用户历史记录表。
   */
  private def queryHistoryByToken(userIDOption: Option[Int])(using PlanContext): IO[Option[List[HistoryRecord]]] = {
    userIDOption match {
      case Some(userID) =>
        IO(logger.info(s"用户合法，开始查询历史记录，用户ID: ${userID}")) >>
          queryUserHistory(userID, rangeL, rangeR).map(Some(_))
      case None =>
        IO(logger.info(s"Token非法，返回空记录")) >>
          IO.pure(None)
    }
  }

  /**
   * 使用用户ID和范围（rangeL, rangeR）从数据库查询用户的历史记录。
   * 按照timestamp从新到旧排序，分页读取。
   */
  private def queryUserHistory(userID: Int, rangeL: Int, rangeR: Int)(using PlanContext): IO[List[HistoryRecord]] = {
    IO(logger.info(s"开始创建查询用户历史记录的SQL，用户ID: ${userID}, 范围: [${rangeL}, ${rangeR}]")) >>
      IO {
        val sql =
          s"""
            SELECT user_id, video_id, timestamp
            FROM ${schemaName}.history_record_table
            WHERE user_id = ?
            ORDER BY timestamp DESC
            OFFSET ? LIMIT ?;
          """
        logger.info(s"SQL为: ${sql}")

        // 计算Offset和Limit
        val offset = rangeL - 1
        val limit = rangeR - rangeL + 1
        logger.info(s"Offset为: ${offset}, Limit为: ${limit}")

        // SQL参数列表
        val parameters = List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Int", offset.toString),
          SqlParameter("Int", limit.toString)
        )

        (sql, parameters)
      }.flatMap { case (sql, parameters) =>
        logger.info("开始执行SQL查询用户历史记录")
        readDBRows(sql, parameters).map { rows =>
          logger.info(s"查询成功，记录数量: ${rows.size}")
          rows.map { row =>
            HistoryRecord(
              userID = decodeField[Int](row, "user_id"),
              videoID = decodeField[Int](row, "video_id"),
              timestamp = decodeField[DateTime](row, "timestamp")
            )
          }
        }
      }
  }
}