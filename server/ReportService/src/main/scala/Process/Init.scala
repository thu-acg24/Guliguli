
package Process


import Common.API.API
import Common.API.PlanContext
import Common.API.TraceID
import Common.DBAPI.initSchema
import Common.DBAPI.writeDB
import Common.ServiceUtils.schemaName
import Global.DBConfig
import Global.GlobalVariables
import Global.ServerConfig
import Process.ProcessUtils.server2DB
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 用于存储被举报的弹幕及相关信息
       * report_id: 举报的唯一ID
       * danmaku_id: 被举报的弹幕ID
       * reporter_id: 举报者用户ID
       * reason: 举报理由
       * status: 举报状态（Pending, Resolved, Rejected）
       * timestamp: 举报时间
       * node_path: 节点路径
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."report_danmaku_table" (
            report_id SERIAL NOT NULL PRIMARY KEY,
            danmaku_id INT NOT NULL,
            reporter_id INT NOT NULL,
            reason TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'Pending',
            timestamp TIMESTAMP NOT NULL,
            node_path TEXT DEFAULT 'guliguliguli/ReportService/ReportService-TableRoot/ReportDanmakuTable'
        );
         
        """,
        List()
      )
      /** 视频举报表，包含举报的基本信息
       * report_id: 举报信息的唯一标识
       * video_id: 被举报的视频ID
       * reporter_id: 举报者用户ID
       * reason: 举报理由
       * status: 举报状态 (Pending, Resolved, Rejected)
       * timestamp: 举报时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."report_video_table" (
            report_id SERIAL NOT NULL PRIMARY KEY,
            video_id INT NOT NULL,
            reporter_id INT NOT NULL,
            reason TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'Pending',
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 举报评论表，保存举报的评论信息
       * report_id: 举报记录的唯一ID
       * comment_id: 被举报的评论ID
       * reporter_id: 举报者用户ID
       * reason: 举报理由
       * status: 举报状态 (Pending, Resolved, Rejected)
       * timestamp: 举报时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."report_comment_table" (
            report_id SERIAL NOT NULL PRIMARY KEY,
            comment_id INT NOT NULL,
            reporter_id INT NOT NULL,
            reason TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'Pending',
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    