package syncfork.send

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  Uri
}
import scala.concurrent.Future
import syncfork.config.Config.{Branch, Owner, Repo, Token}

object Send {

  /** Sends a request to the GitHub API to sync a forked repository with its
    * upstream.
    *
    * @param token
    *   The GitHub personal access token for authentication.
    * @param owner
    *   The owner of the forked repository.
    * @param repo
    *   The name of the forked repository.
    * @param branch
    *   The name of the branch to sync.
    * @param system
    *   The ActorSystem used for making the HTTP request.
    * @return
    *   A Future containing the HTTP response from the GitHub API.
    */
  def sync(token: Token, owner: Owner, repo: Repo, branch: Branch)(implicit
      system: ActorSystem[_]
  ): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(s"https://api.github.com/repos/$owner/$repo/merge-upstream"),
      headers = List(
        RawHeader("Accept", "application/vnd.github+json"),
        RawHeader("Authorization", s"Bearer $token"),
        RawHeader("X-GitHub-Api-Version", "2022-11-28")
      ),
      entity =
        HttpEntity(ContentTypes.`application/json`, s"""{"branch":"$branch"}""")
    )
    Http().singleRequest(request)
  }
}
