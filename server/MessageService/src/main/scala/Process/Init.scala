
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
            /** 包含私信和通知信息的表
       * message_id: 唯一标识每条私信的主键ID
       * sender_id: 私信发送者的用户ID
       * receiver_id: 私信接收者的用户ID
       * content: 私信内容
       * timestamp: 私信发送时间
       * is_notification: 是否为通知
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."message_table" (
            message_id SERIAL NOT NULL PRIMARY KEY,
            sender_id INT NOT NULL,
            receiver_id INT NOT NULL,
            content TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL,
            is_notification BOOLEAN NOT NULL
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
    