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
import controllers.routes
import models.NormalMode
import play.api.libs.json.Json
import play.api.mvc.request.{ Cell, RequestAttrKey }
import play.api.mvc.{ Filter, RequestHeader, Result, SessionCookieBaker }

import scala.concurrent.{ ExecutionContext, Future }

class BackLinkFilter(
  override val mat: Materializer,
  sessionCookieBaker: SessionCookieBaker,
  implicit val ec: ExecutionContext
) extends Filter {

  @Inject
  def this(mat: Materializer, ec: ExecutionContext, sessionCookieBaker: SessionCookieBaker) =
    this(mat, sessionCookieBaker, ec)

  private val supportedPages = Set(
    routes.AccountingPeriodController.onPageLoad(NormalMode).path,
    routes.CheckYourAnswersController.onPageLoad.path,
    routes.DistributionController.onPageLoad(NormalMode).path,
    routes.DistributionsIncludedController.onPageLoad(NormalMode).path,
    routes.AssociatedCompaniesController.onPageLoad(NormalMode).path,
    routes.ResultsPageController.onPageLoad().path,
    routes.FullResultsPageController.onPageLoad().path,
    routes.IndexController.onPageLoad.path,
    routes.TaxableProfitController.onPageLoad(NormalMode).path,
    routes.PDFAddCompanyDetailsController.onPageLoad().path(),
    routes.PDFMetadataController.onPageLoad().path,
    routes.PDFController.onPageLoad().path,
    routes.TwoAssociatedCompaniesController.onPageLoad(NormalMode).path
  )

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] =
    if (rh.method == "GET" && supportedPages.contains(rh.path)) {
      val session = rh.session
      val visitedLinks = session.get(BackLinkFilter.visitedLinks) match {
        case Some(value) =>
          Json.parse(value).as[List[String]]
        case None =>
          List.empty
      }
      val isBackLinkClicked = rh.queryString.contains("back")
      val isRefererChangePage = rh.headers.get("Referer").exists(_.contains("/change-"))
      val updatedVisitedLinks = if (isBackLinkClicked) {
        if (isRefererChangePage) {
          visitedLinks
        } else {
          if (visitedLinks.nonEmpty)
            visitedLinks.tail
          else visitedLinks
        }
      } else {
        if (visitedLinks.headOption.contains(rh.path)) visitedLinks else rh.path :: visitedLinks
      }
      val updatedSession = session + (BackLinkFilter.visitedLinks -> Json.toJson(updatedVisitedLinks).toString())
      f(
        rh.addAttr(RequestAttrKey.Session, Cell(updatedSession))
      ).map(_.withSession(updatedSession))(ec)
    } else {
      f(rh)
    }
}

object BackLinkFilter {
  val visitedLinks = "visitedLinks"
}
