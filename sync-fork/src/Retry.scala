package syncfork.retry

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.duration.*
import org.slf4j.LoggerFactory
import syncfork.send.Send
import syncfork.config.Config
import syncfork.config.Config.GithubConfig

object Retry {
  private val maxRetries: Int = 10
  private val initialBackoff: FiniteDuration = 10.seconds
  private val maxBackoff: FiniteDuration = 10.seconds
  private val logger = LoggerFactory.getLogger(this.getClass)

  private def syncWithRetry(
      config: GithubConfig,
      attempt: Int = 1,
      backoff: FiniteDuration = initialBackoff
  )(using
      system: ActorSystem[?],
      ec: ExecutionContext
  ): Future[HttpResponse] = {
    logger.info(s"Executing sync (Attempt $attempt/$maxRetries)")

    Send
      .sync(
        token = config.token,
        owner = config.owner,
        repo = config.repo,
        branch = config.branch
      )
      .transformWith {
        case Success(response) if response.status.isSuccess =>
          logger.info(s"Sync successfully (Attempt $attempt/$maxRetries)")
          Future.successful(response)

        case Success(response) =>
          val status = response.status
          logger.warn(
            s"Return error status: $status (Attempt $attempt/$maxRetries)"
          )

          if (attempt < maxRetries) {
            val nextBackoff =
              (backoff * 2).min(maxBackoff) // Retry with exponential backoff
            logger.info(s"Will retry after $nextBackoff...")

            // Retry with akka scheduler
            val retryFuture = Promise[HttpResponse]()
            summon[ActorSystem[Nothing]].scheduler.scheduleOnce(
              nextBackoff,
              () => {
                syncWithRetry(config, attempt + 1, nextBackoff)
                  .onComplete(retryFuture.complete)
              }
            )
            retryFuture.future
          } else {
            logger.error(s"Reached max retry attempts, giving up sync")
            Future.successful(response) // Return the last response
          }

        case Failure(exception) =>
          logger.warn(
            s"Fail to sync: ${exception.getMessage} (Attempt $attempt/$maxRetries)"
          )

          if (attempt < maxRetries) {
            val nextBackoff =
              (backoff * 2).min(maxBackoff) // Retry with exponential backoff
            logger.warn(s"Will retry after $nextBackoff...")

            // Retry
            val retryFuture = Promise[HttpResponse]()
            summon[ActorSystem[Nothing]].scheduler.scheduleOnce(
              nextBackoff,
              () => {
                syncWithRetry(config, attempt + 1, nextBackoff)
                  .onComplete(retryFuture.complete)
              }
            )
            retryFuture.future
          } else {
            logger.error(s"Reached max retry attempts, giving up sync")
            Future.failed(exception) // Failed
          }
      }
  }

  def performSync(
      config: GithubConfig
  )(using system: ActorSystem[?], ec: ExecutionContext): Unit = {
    syncWithRetry(config).onComplete {
      case Success(response) =>
        // Only record the last response
        if (response.status.isSuccess) {
          logger.info("Finished syncing")
        } else {
          logger.error(s"Fail to sync: ${response.entity}")
        }
      case Failure(exception) =>
        logger.error(s"Fail to sync: ${exception.getMessage}")
    }
  }
}
