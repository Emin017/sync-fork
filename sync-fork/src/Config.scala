package syncfork.config

import com.typesafe.config.Config

object Config {

  /** Token is an opaque type representing a GitHub personal access token.
    *
    * @param value
    *   The string value of the token.
    */
  opaque type Token = String

  object Token {
    def apply(value: String): Token = value

    given Conversion[Token, String] = _.value

    extension (t: Token) def value: String = t
  }

  /** Owner is an opaque type representing the owner of a GitHub repository.
    *
    * @param value
    *   The string value of the owner.
    */
  opaque type Owner = String

  object Owner {
    def apply(value: String): Owner = value

    given Conversion[Owner, String] = _.value

    extension (t: Owner) def value: String = t
  }

  /** Repo is an opaque type representing a GitHub repository.
    *
    * @param value
    *   The string value of the repository.
    */
  opaque type Repo = String

  object Repo {
    def apply(value: String): Repo = value

    given Conversion[Repo, String] = _.value

    extension (t: Repo) def value: String = t
  }

  /** Branch is an opaque type representing a GitHub branch.
    *
    * @param value
    *   The string value of the branch.
    */
  opaque type Branch = String

  object Branch {
    def apply(value: String): Branch = value

    given Conversion[Branch, String] = _.value

    extension (t: Branch) def value: String = t
  }

  /** GithubConfig is an opaque type representing the configuration for a GitHub
    * repository.
    *
    * @param token
    *   The GitHub personal access token.
    * @param owner
    *   The owner of the repository.
    * @param repo
    *   The name of the repository.
    * @param branch
    *   The name of the branch.
    */
  opaque type GithubConfig = (Token, Owner, Repo, Branch)

  object GithubConfig {
    def apply(
        token: Token,
        owner: Owner,
        repo: Repo,
        branch: Branch
    ): GithubConfig =
      (token, owner, repo, branch)
  }

  extension (config: GithubConfig) {
    def token: Token = config._1
    def owner: Owner = config._2
    def repo: Repo = config._3
    def branch: Branch = config._4
  }

  /** fromConfig is a method that reads the GitHub configuration from a Typesafe
    * Config object.
    *
    * @param config
    *   The Typesafe Config object containing the GitHub configuration.
    * @return
    *   A GithubConfig object containing the GitHub configuration.
    */
  def fromConfig(config: Config): GithubConfig = {
    val githubConfig = config.getConfig("github")
    GithubConfig(
      token = githubConfig.getString("token"),
      owner = githubConfig.getString("owner"),
      repo = githubConfig.getString("repo"),
      branch = githubConfig.getString("branch")
    )
  }
}
