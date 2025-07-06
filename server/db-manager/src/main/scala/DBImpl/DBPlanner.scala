package DBImpl

import Common.API.{PlanContext, TraceID}
import io.circe.{Decoder, Encoder, Json}
import cats.effect.{IO, Ref}

import java.sql.Connection

trait DBPlanner[ReturnType]:
  def planWithConnection(connection: Connection)(using PlanContext): IO[ReturnType]

  def send(using PlanContext): IO[ReturnType] = callDBAPI[ReturnType](this)
