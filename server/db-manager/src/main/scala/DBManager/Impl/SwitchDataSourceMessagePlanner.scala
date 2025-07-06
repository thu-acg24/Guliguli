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

      // Step 2: Create the new database if it doesn't exist
      initNewDatabase <- createDataSource(dbConfig, "postgres").flatMap { dataSource =>
        IO {
          val connection = dataSource.getConnection
          initDB(connection, projectName.toLowerCase())
          connection.close()
          dataSource.close()
        }
      }
      _ <- createDataSource(dbConfig, "postgres").flatMap { dataSource =>
        IO {
          val conn = dataSource.getConnection
          try {
            val stmt = conn.createStatement()
            stmt.execute(
              """
                |CREATE TABLE IF NOT EXISTS projectName (
                |  name TEXT NOT NULL
                |)
                |""".stripMargin
            )
            stmt.execute("TRUNCATE TABLE projectName")
            val ps = conn.prepareStatement("INSERT INTO projectName (name) VALUES (?)")
            ps.setString(1, projectName)
            ps.executeUpdate()
            ps.close()
            stmt.close()
          } finally {
            conn.close()
            dataSource.close()
          }
        }
      }

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