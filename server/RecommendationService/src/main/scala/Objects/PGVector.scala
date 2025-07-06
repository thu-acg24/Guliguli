package Objects

import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.parser.*

case class PGVector(values: Vector[Float]) {
  require(values.nonEmpty, "Vector must not be empty")

  def +(that: PGVector): PGVector =
    PGVector(this.values.zip(that.values).map { case (a, b) => a + b })

  def -(that: PGVector): PGVector =
    PGVector(this.values.zip(that.values).map { case (a, b) => a - b })

  def *(scalar: Float): PGVector =
    PGVector(this.values.map(_ * scalar))

  def dot(that: PGVector): Float =
    this.values.zip(that.values).map { case (a, b) => a * b }.sum

  def magnitude: Float =
    math.sqrt(this.values.map(x => x * x).sum).toFloat

  def normalize: PGVector = {
    val mag = magnitude
    if (mag == 0f) this
    else this * (1f / mag)
  }

  override def toString: String = values.mkString("[", ",", "]")
}

case object PGVector {

  // Circe JSON support

  implicit val encoder: Encoder[PGVector] = Encoder.instance(v => v.values.asJson)
  implicit val decoder: Decoder[PGVector] = Decoder.instance { cursor =>
    cursor.as[Vector[Float]].map(PGVector(_))
  }
}