
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
      /** 包含私信信息的表
       * message_id: 唯一标识每条私信的主键ID
       * sender_id: 私信发送者的用户ID
       * receiver_id: 私信接收者的用户ID
       * content: 私信内容
       * send_time: 私信发送时间
       * unread: 是否未读
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."message_table" (
            message_id SERIAL NOT NULL PRIMARY KEY,
            sender_id INT NOT NULL,
            receiver_id INT NOT NULL,
            content TEXT NOT NULL,
            send_time TIMESTAMP NOT NULL,
            unread BOOLEAN NOT NULL DEFAULT TRUE
        );
        """,
        List()
      )
      /** 包含通知信息的表
       * notification_id: 唯一标识每条私信的主键ID
       * receiver_id: 私信接收者的用户ID
       * content: 私信内容
       * send_time: 私信发送时间
       * unread: 是否未读
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."notification_table" (
            notification_id SERIAL NOT NULL PRIMARY KEY,
            receiver_id INT NOT NULL,
            title TEXT NOT NULL,
            content TEXT NOT NULL,
            send_time TIMESTAMP NOT NULL,
            unread BOOLEAN NOT NULL DEFAULT TRUE
        );
        """,
        List()
      )
      /** 包含回复评论通知信息的表
       * notice_id: 唯一标识每条通知的主键ID
       * sender_id: 回复发送者的用户ID
       * receiver_id: 被回复者的用户ID
       * content: 回复内容
       * comment_id: 回复ID
       * original_content: 原评论内容
       * original_comment_id: 原评
       * video_id: 视频ID
       * send_time: 私信发送时间
       * unread: 是否未读
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."reply_notice_table" (
            notice_id SERIAL NOT NULL PRIMARY KEY,
            sender_id INT NOT NULL,
            receiver_id INT NOT NULL,
            content TEXT NOT NULL,
            comment_id INT NOT NULL,
            original_content TEXT NOT NULL,
            original_comment_id INT NOT NULL,
            video_id INT NOT NULL,
            send_time TIMESTAMP NOT NULL,
            unread BOOLEAN NOT NULL DEFAULT TRUE
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
    