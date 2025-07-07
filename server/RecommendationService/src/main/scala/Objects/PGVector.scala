package Objects

import cats.effect.IO
import cats.effect.std.Random
import cats.syntax.all._
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.parser.*

import java.security.MessageDigest

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


  private val DEFAULT_SALT = "mySecureSalt123!@#"

  def fromString(str: String, salt: String = DEFAULT_SALT, dim: Int = 384): IO[PGVector] = {
    require(dim > 0, "Dimension must be positive")
    // 创建确定性种子
    val seed = {
      val saltedInput = str + salt
      val md = MessageDigest.getInstance("SHA-256")
      val hashBytes = md.digest(saltedInput.getBytes("UTF-8"))
      BigInt(1, hashBytes).toLong
    }

    // 使用种子创建随机向量
    for {
      random <- Random.scalaUtilRandomSeedLong[IO](seed)
      values <- Vector.fill(dim)(random.nextFloat).sequence
    } yield PGVector(values)
  }
}