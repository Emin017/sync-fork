package syncfork.config

import utest._
import com.typesafe.config.ConfigFactory
import java.io.StringReader
import syncfork.config.Config._

object ConfigTests extends TestSuite {
  val tests: Tests = Tests {
    // Test opaque types creation and access
    test("Token creation and access") {
      val token = Token("test-token")
      assert(token.value == "test-token")
    }

    test("Owner creation and access") {
      val owner = Owner("test-owner")
      assert(owner.value == "test-owner")
    }

    test("Repo creation and access") {
      val repo = Repo("test-repo")
      assert(repo.value == "test-repo")
    }

    test("Branch creation and access") {
      val branch = Branch("test-branch")
      assert(branch.value == "test-branch")
    }

    // Test GithubConfig creation and access
    test("GithubConfig creation and access") {
      val token = Token("test-token")
      val owner = Owner("test-owner")
      val repo = Repo("test-repo")
      val branch = Branch("test-branch")

      val config = GithubConfig(token, owner, repo, branch)

      assert(config.token == token)
      assert(config.owner == owner)
      assert(config.repo == repo)
      assert(config.branch == branch)
    }

    // Test implicit conversion from opaque types to String
    test("Implicit conversion from opaque types to String") {
      val token: Token = Token("test-token")
      val owner: Owner = Owner("test-owner")
      val repo: Repo = Repo("test-repo")
      val branch: Branch = Branch("test-branch")

      val tokenStr: String = token
      val ownerStr: String = owner
      val repoStr: String = repo
      val branchStr: String = branch

      assert(tokenStr == "test-token")
      assert(ownerStr == "test-owner")
      assert(repoStr == "test-repo")
      assert(branchStr == "test-branch")
    }

    // Test fromConfig method
    test("fromConfig correctly parses Typesafe Config") {
      val configString =
        """
        github {
          token = "test-token"
          owner = "test-owner"
          repo = "test-repo"
          branch = "test-branch"
        }
      """

      val typesafeConfig = ConfigFactory.parseReader(new StringReader(configString))
      val githubConfig = Config.fromConfig(typesafeConfig)

      assert(githubConfig.token.value == "test-token")
      assert(githubConfig.owner.value == "test-owner")
      assert(githubConfig.repo.value == "test-repo")
      assert(githubConfig.branch.value == "test-branch")
    }

    // Test fromConfig throws exception when required config is missing
    test("fromConfig throws exception when required config is missing") {
      val configString =
        """
        github {
          token = "test-token"
          owner = "test-owner"
          // missing repo and branch
        }
      """

      val typesafeConfig = ConfigFactory.parseReader(new StringReader(configString))

      val exception = intercept[com.typesafe.config.ConfigException.Missing] {
        Config.fromConfig(typesafeConfig)
      }
      // Check if the exception message contains "repo" or "branch"
      assert(exception.getMessage.contains("repo") || exception.getMessage.contains("branch"))
    }
  }
}