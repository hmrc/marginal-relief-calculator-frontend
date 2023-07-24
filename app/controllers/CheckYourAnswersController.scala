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

package controllers

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.{AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, TwoAssociatedCompaniesForm}
import models.requests.DataRequest
import models.{Distribution, UserAnswers}
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import providers.AssociatedCompaniesParametersProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  associatedCompaniesParametersProvider: AssociatedCompaniesParametersProvider
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  case class CheckYourAnswersRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    associatedCompanies: Option[AssociatedCompaniesForm],
    twoAssociatedCompaniesForm: Option[TwoAssociatedCompaniesForm],
    userAnswers: UserAnswers,
    request: Request[A]
  ) extends WrappedRequest[A](request)

  private val requireDomainData = new ActionRefiner[DataRequest, CheckYourAnswersRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, CheckYourAnswersRequiredParams[A]]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage),
          request.userAnswers.get(AssociatedCompaniesPage),
          request.userAnswers.get(TwoAssociatedCompaniesPage)
        ) match {
          case (
                Some(accPeriod),
                Some(taxableProfit),
                Some(distribution),
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                twoAssociatedCompanies
              ) =>
            Right(
              CheckYourAnswersRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                twoAssociatedCompanies,
                request.userAnswers,
                request
              )
            )
          case _ => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      for {
        askAssociatedParameter <- associatedCompaniesParametersProvider.associatedCompaniesParameters(
                                    request.accountingPeriod.accountingPeriodStartDate,
                                    request.accountingPeriod.accountingPeriodEndDateOrDefault
                                  )
      } yield {
        val list = SummaryListViewModel(
          AccountingPeriodSummary.row(request.userAnswers) ++
            TaxableProfitSummary.row(request.userAnswers) ++
            DistributionSummary.row(request.userAnswers) ++
            AssociatedCompaniesSummary.row(request.userAnswers, askAssociatedParameter) ++
            TwoAssociatedCompaniesSummary.row(request.userAnswers, askAssociatedParameter)
        )
        Ok(view(list, routes.ResultsPageController.onPageLoad().url))
      }
    }
}
