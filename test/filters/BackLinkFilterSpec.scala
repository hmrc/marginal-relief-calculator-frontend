/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.routes
import models.NormalMode
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.http.{ HttpFilters, Status }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{ Result, SessionCookieBaker }
import play.api.test.{ FakeRequest, Helpers }
import play.api.test.Helpers.{ defaultAwaitTimeout, route, session, status, writeableOf_AnyContentAsEmpty }
import play.api.{ Application, BuiltInComponents, BuiltInComponentsFromContext, NoHttpFiltersComponents }

import scala.concurrent.{ ExecutionContext, Future }

object BackLinkFilterSpec {
  class Filters @Inject() (backlinkFilter: BackLinkFilter) extends HttpFilters {
    def filters = Seq(backlinkFilter)
  }

  class TestBackLinkFilter @Inject() (
    override val mat: Materializer,
    sessionCookieBaker: SessionCookieBaker,
    ec: ExecutionContext
  ) extends BackLinkFilter(mat, sessionCookieBaker, ec)

}

class BackLinkFilterSpec
    extends AnyFreeSpec with Matchers with OneAppPerSuiteWithComponents with ScalaFutures with IntegrationPatience
    with TableDrivenPropertyChecks {

  override def components: BuiltInComponents = new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
    import play.api.mvc.Results
    import play.api.routing.Router
    import play.api.routing.sird._
    lazy val router: Router = Router.from {
      case GET(p"/marginal-relief-calculator") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/accounting-period") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/taxable-profit") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/distribution") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/distributions-included") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/associated-companies") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/check-your-answers") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/results-page") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/full-results-page") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/pdf-meta-data") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/pdf") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case GET(p"/marginal-relief-calculator/unsupported") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
      case POST(p"/marginal-relief-calculator") =>
        defaultActionBuilder.apply { _ =>
          Results.Ok
        }
    }
  }

  import BackLinkFilterSpec._

  override lazy val app: Application = {
    import play.api.inject._
    new GuiceApplicationBuilder()
      .overrides(
        bind[HttpFilters].to[Filters],
        bind[BackLinkFilter].to[TestBackLinkFilter]
      )
      .router(components.router)
      .build()
  }

  "BackLinkFilter" - {

    "should not add to session attribute visitedLinks for POST request" in {
      val Some(result) = route(app, FakeRequest(Helpers.POST, "/marginal-relief-calculator"))
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe None
    }

    "should not add to session attribute visitedLinks for GET request, for unsupported pages" in {
      val Some(result) = route(app, FakeRequest(Helpers.GET, "/marginal-relief-calculator/unsupported"))
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe None
    }

    "should add to session attribute visitedLinks for GET request of all supported pages" in {
      val table = Table(
        ("path", "visitedLinks"),
        (
          routes.AccountingPeriodController.onPageLoad(NormalMode).path,
          List(routes.AccountingPeriodController.onPageLoad(NormalMode).path)
        ),
        (routes.CheckYourAnswersController.onPageLoad.path, List(routes.CheckYourAnswersController.onPageLoad.path)),
        (
          routes.DistributionController.onPageLoad(NormalMode).path,
          List(routes.DistributionController.onPageLoad(NormalMode).path)
        ),
        (
          routes.DistributionsIncludedController.onPageLoad(NormalMode).path,
          List(routes.DistributionsIncludedController.onPageLoad(NormalMode).path)
        ),
        (
          routes.AssociatedCompaniesController.onPageLoad(NormalMode).path,
          List(routes.AssociatedCompaniesController.onPageLoad(NormalMode).path)
        ),
        (routes.ResultsPageController.onPageLoad().path, List(routes.ResultsPageController.onPageLoad().path)),
        (routes.FullResultsPageController.onPageLoad().path, List(routes.FullResultsPageController.onPageLoad().path)),
        (routes.IndexController.onPageLoad.path, List(routes.IndexController.onPageLoad.path)),
        (
          routes.TaxableProfitController.onPageLoad(NormalMode).path,
          List(routes.TaxableProfitController.onPageLoad(NormalMode).path)
        ),
        (routes.PDFMetadataController.onPageLoad().path, List(routes.PDFMetadataController.onPageLoad().path)),
        (routes.PDFController.onPageLoad().path, List(routes.PDFController.onPageLoad().path))
      )
      forAll(table) { (path, expected) =>
        val Some(result) = route(app, FakeRequest(Helpers.GET, path))
        status(result) shouldBe Status.OK
        visitedLinksFromSession(result) shouldBe Some(expected)
      }
    }

    "should update session attribute visitedLinks if there it exists" in {
      val Some(result) = route(
        app,
        FakeRequest(Helpers.GET, "/marginal-relief-calculator/accounting-period").withSession(
          "visitedLinks" -> Json.toJson(List("/marginal-relief-calculator")).toString
        )
      )
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe Some(
        List("/marginal-relief-calculator/accounting-period", "/marginal-relief-calculator")
      )
    }

    "should remove the head of the visitedLinks list, when back=true and source isn't a change page" in {
      val Some(result) = route(
        app,
        FakeRequest(Helpers.GET, "/marginal-relief-calculator/accounting-period?back=true")
          .withSession(
            "visitedLinks" -> Json
              .toJson(
                List(
                  "/marginal-relief-calculator/taxable-profit",
                  "/marginal-relief-calculator/accounting-period",
                  "/marginal-relief-calculator"
                )
              )
              .toString
          )
      )
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe Some(
        List("/marginal-relief-calculator/accounting-period", "/marginal-relief-calculator")
      )
    }

    "should not remove the head of the visitedLinks list, when back=true and visitedLinks list unavailable" in {
      val Some(result) = route(
        app,
        FakeRequest(Helpers.GET, "/marginal-relief-calculator/accounting-period?back=true")
      )
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe Some(
        List()
      )
    }

    "should not remove the head of the visitedLinks list, when back=true and source is a change page" in {
      val Some(result) = route(
        app,
        FakeRequest(Helpers.GET, "/marginal-relief-calculator/check-your-answers?back=true")
          .withHeaders("Referer" -> "/marginal-relief-calculator/change-associated-companies")
          .withSession(
            "visitedLinks" -> Json
              .toJson(
                List(
                  "/marginal-relief-calculator/associated-companies",
                  "/marginal-relief-calculator/distribution",
                  "/marginal-relief-calculator/taxable-profit",
                  "/marginal-relief-calculator/accounting-period",
                  "/marginal-relief-calculator"
                )
              )
              .toString
          )
      )
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe Some(
        List(
          "/marginal-relief-calculator/associated-companies",
          "/marginal-relief-calculator/distribution",
          "/marginal-relief-calculator/taxable-profit",
          "/marginal-relief-calculator/accounting-period",
          "/marginal-relief-calculator"
        )
      )
    }

    "should not update visitedLinks if the head is already the current path" in {
      val Some(result) = route(
        app,
        FakeRequest(Helpers.GET, "/marginal-relief-calculator/accounting-period")
          .withSession(
            "visitedLinks" -> Json
              .toJson(List("/marginal-relief-calculator/accounting-period", "/marginal-relief-calculator"))
              .toString
          )
      )
      status(result) shouldBe Status.OK
      visitedLinksFromSession(result) shouldBe Some(
        List("/marginal-relief-calculator/accounting-period", "/marginal-relief-calculator")
      )
    }
  }

  private def visitedLinksFromSession(result: Future[Result]): Option[List[String]] =
    session(result).get("visitedLinks").map(v => Json.parse(v).as[List[String]])
}
