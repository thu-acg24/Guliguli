
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
       /** 弹幕表，用于存储弹幕的基本信息
       * danmaku_id: 弹幕的唯一ID (主键)
       * content: 弹幕内容
       * video_id: 所属视频的ID
       * author_id: 发布弹幕的用户ID
       * danmaku_color: 弹幕颜色 (如 #FFFFFF)
       * time_in_video: 弹幕出现的时间点（秒）
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."danmaku_table" (
            danmaku_id SERIAL NOT NULL PRIMARY KEY,
            content TEXT NOT NULL,
            video_id INT NOT NULL,
            author_id INT NOT NULL,
            danmaku_color TEXT NOT NULL,
            time_in_video REAL NOT NULL
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
    