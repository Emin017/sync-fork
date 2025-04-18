package syncfork

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.duration.*
import org.slf4j.LoggerFactory
import syncfork.send.Send
import syncfork.config.Config

object SyncFork {
  @main
  def run(): Unit = {

    given ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SyncForkedRepo")

    given ExecutionContext = summon[ActorSystem[Nothing]].executionContext

    val logger = LoggerFactory.getLogger(getClass)
    // Starting
    // Load configuration
    val config = ConfigFactory.load()
    val githubConfig = Config.fromConfig(config)
    val syncInterval = config.getDuration("sync.interval").getSeconds.seconds
    val initialDelay =
      config.getDuration("sync.initialDelay").getSeconds.seconds

    logger.info(
      s"SyncFork starting - syncing ${githubConfig.owner}/${githubConfig.repo} branch ${githubConfig.branch}"
    )
    logger.info(
      s"Scheduled to run every $syncInterval with initial delay of $initialDelay"
    )

    // TODO: Replace this api with retry logic
    def performSync(): Unit = {
      Send
        .sync(
          token = githubConfig.token,
          owner = githubConfig.owner,
          repo = githubConfig.repo,
          branch = githubConfig.branch
        )
        .onComplete {
          case Success(response) =>
            response.status match {
              case status if status.isSuccess =>
                logger.info("Successfully synced")
              case status => logger.error(s"Fail to sync: $status")
            }
          case Failure(exception) => logger.error(s"Failed to sync: $exception")
        }
    }

    summon[ActorSystem[Nothing]].scheduler.scheduleWithFixedDelay(
      initialDelay = initialDelay,
      delay = syncInterval
    )(() => performSync())
  }
}
