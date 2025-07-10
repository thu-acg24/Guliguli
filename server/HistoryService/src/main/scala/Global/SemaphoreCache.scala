package Global

import com.github.benmanes.caffeine.cache.{Caffeine, Cache}
import cats.effect.IO
import cats.effect.std.Semaphore
import java.util.concurrent.TimeUnit

class SemaphoreCache(maxPermits: Int) {
  // Caffeine cache 自动过期：10分钟没访问自动删除
  private val cache: Cache[String, Semaphore[IO]] = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build[String, Semaphore[IO]]()

  def getSemaphore(token: String): IO[Semaphore[IO]] = {
    Option(cache.getIfPresent(token)) match {
      case Some(sem) => IO.pure(sem)
      case None =>
        for {
          newSem <- Semaphore[IO](maxPermits.toLong)
          _ = cache.put(token, newSem)
        } yield newSem
    }
  }

  def withPermit[A](token: String)(fa: IO[A]): IO[A] =
    for {
      sem <- getSemaphore(token)
      res <- sem.permit.use(_ => fa)
    } yield res
}