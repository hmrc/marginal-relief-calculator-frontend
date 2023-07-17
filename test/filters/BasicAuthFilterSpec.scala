/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package filters

import akka.stream.Materializer
import com.google.inject.Inject
import config.FrontendAppConfig
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import play.api.http.{ DefaultHttpFilters, HeaderNames, HttpFilters, Status }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.Helpers.{ GET, defaultAwaitTimeout, header, route, status, writeableOf_AnyContentAsEmpty }
import play.api.{ Application, Configuration }
import scala.concurrent.ExecutionContext

object BasicAuthFilterSpec {
  class Filters @Inject() (basicAuthFilter: BasicAuthFilter) extends DefaultHttpFilters(basicAuthFilter)
  lazy val config: Configuration = Configuration(
    ConfigFactory
      .parseString("""
                     |auth {
                     |   enabled = true
                     |   basic {
                     |      realm = "Marginal Relief Calculator Test"
                     |      username = "test-user"
                     |      password = "test-password"
                     |   }
                     |}
                     |""".stripMargin)
      .withFallback(ConfigFactory.load())
  )
  lazy val frontendAppConfig: FrontendAppConfig = new FrontendAppConfig(config)
  class TestBasicAuthFilter @Inject() (
    override val mat: Materializer,
    ec: ExecutionContext,
    scb: SessionCookieBaker
  ) extends BasicAuthFilter(frontendAppConfig, mat)
}

class BasicAuthFilterSpec
    extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience with GuiceOneAppPerSuite {

  import BasicAuthFilterSpec._

  override lazy val app: Application = {

    import play.api.inject._

    new GuiceApplicationBuilder()
      .loadConfig(config)
      .overrides(
        bind[HttpFilters].to[Filters],
        bind[BasicAuthFilter].to[TestBasicAuthFilter]
      )
      .build()
  }

  private val incorrectUsernameAuthHeader = "Basic d3Jvbmc6bXJj"
  private val incorrectPasswordAuthHeader = "Basic bXJjOndyb25n"
  private val correctAuthHeader = "Basic dGVzdC11c2VyOnRlc3QtcGFzc3dvcmQ="

  ".apply should" - {

    "request WWW-authentication if authorization header is not present" in {
      val maybeResult = route(app, FakeRequest(GET, "/"))
      maybeResult should not be None
      maybeResult.map { result =>
        status(result) shouldBe Status.UNAUTHORIZED
        header(HeaderNames.WWW_AUTHENTICATE, result) shouldBe Some(s"""Basic realm="Marginal Relief Calculator Test"""")
      }
    }

    "request WWW-authentication if incorrect username sent in authorization header" in {
      val maybeWrongUsrResult =
        route(app, FakeRequest(GET, "/").withHeaders("Authorization" -> incorrectUsernameAuthHeader))
      maybeWrongUsrResult should not be None
      maybeWrongUsrResult.map { wrongUsrResult =>
        status(wrongUsrResult) shouldBe Status.UNAUTHORIZED
        header(HeaderNames.WWW_AUTHENTICATE, wrongUsrResult) shouldBe Some(
          s"""Basic realm="Marginal Relief Calculator Test""""
        )
      }
    }
    "request WWW-authentication if incorrect password sent in authorization header" in {
      val maybeWrongPassResult =
        route(app, FakeRequest(GET, "/").withHeaders("Authorization" -> incorrectPasswordAuthHeader))
      maybeWrongPassResult should not be None
      maybeWrongPassResult.map { wrongPassResult =>
        status(wrongPassResult) shouldBe Status.UNAUTHORIZED
        header(HeaderNames.WWW_AUTHENTICATE, wrongPassResult) shouldBe Some(
          s"""Basic realm="Marginal Relief Calculator Test""""
        )
      }
    }

    "continue if username and password are correct in authorization header" in {
      val result = route(app, FakeRequest(GET, "/").withHeaders("Authorization" -> correctAuthHeader))
      result should not be None
      result.map(status) shouldBe Some(Status.NOT_FOUND)
    }

    "continue if path is /ping/ping" in {
      val result = route(app, FakeRequest(GET, "/ping/ping"))
      result should not be None
      result.map(status) shouldBe Some(Status.OK)
    }
  }

}
