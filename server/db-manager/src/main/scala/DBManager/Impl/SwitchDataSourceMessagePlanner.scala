package DBManager.Impl

import Global.DBConfig
import DBManager.Process.SourceUtils.{createDataSource, initDB}
import cats.effect.{IO, Ref}
import com.zaxxer.hikari.HikariDataSource

case class SwitchDataSourceMessagePlanner(projectName: String) {
  def plan(dataSourceRef: Ref[IO, Option[HikariDataSource]], dbConfig: DBConfig): IO[String] = {
    for {
      // Step 1: Close the old datasource if it exists
      closeOldDataSource <- for {
        oldDataSourceOpt <- dataSourceRef.get
        _ <- oldDataSourceOpt match {
          case Some(oldDataSource) => IO(oldDataSource.close()) // Close the old datasource
          case None => IO.unit // No existing datasource, nothing to close
        }
      } yield ()

      setNewDataSource <- {
        // Acquire a new datasource
        createDataSource(dbConfig, projectName.toLowerCase()).flatMap { newDataSource =>
          // Close the old datasource and set the new one
          dataSourceRef.set(Some(newDataSource)) *> IO.pure("Data source switched successfully.")
        }
      }

      // Execute all steps in sequence
    } yield setNewDataSource
  }
}