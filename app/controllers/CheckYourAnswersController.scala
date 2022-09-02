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

package controllers

import com.google.inject.Inject
import controllers.actions.{ DataRequiredAction, DataRetrievalAction, IdentifierAction }
import models.Distribution
import models.requests.DataRequest
import pages._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{ AccountingPeriodSummary, AssociatedCompaniesSummary, DistributionSummary, TaxableProfitSummary }
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ ExecutionContext, Future }

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val mandatoryParamsCheck = new ActionFilter[DataRequest] {
    override protected def filter[A](
      request: DataRequest[A]
    ): Future[Option[Result]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage),
          request.userAnswers.get(AssociatedCompaniesPage)
        ) match {
          case (Some(_), Some(_), Some(distribution), maybeDistributionsIncluded, Some(_))
              if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            None
          case _ => Some(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen mandatoryParamsCheck) {
    implicit request =>
      val list = SummaryListViewModel(
        AccountingPeriodSummary.row(request.userAnswers) ++
          TaxableProfitSummary.row(request.userAnswers) ++
          DistributionSummary.row(request.userAnswers) ++
          AssociatedCompaniesSummary.row(request.userAnswers)
      )
      Ok(view(list, routes.ResultsPageController.onPageLoad().url))
  }
}
