
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
      /** 历史记录表，包含用户观看历史记录的信息
       * history_id: 历史记录ID
       * user_id: 浏览记录所属用户ID
       * video_id: 浏览过的视频ID
       * view_time: 浏览发生时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."history_record_table" (
            history_id SERIAL NOT NULL PRIMARY KEY,
            user_id INT NOT NULL,
            video_id INT NOT NULL,
            view_time TIMESTAMP NOT NULL
        );
        """,
        List()
      )
      _ <- writeDB(
        s"""CREATE INDEX IF NOT EXISTS idx_history_view_time ON "$schemaName"."history_record_table"(view_time);
           |""".stripMargin, List())
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    