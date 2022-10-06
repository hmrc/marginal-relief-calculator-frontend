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

import connectors.MarginalReliefCalculatorConnector
import controllers.actions._
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.requests.DataRequest
import models.{ Distribution, UserAnswers }
import pages._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ResultsPageView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ResultsPageController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ResultsPageView,
  marginalReliefCalculatorConnector: MarginalReliefCalculatorConnector
)(implicit val ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  case class ResultsPageRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    associatedCompanies: Option[AssociatedCompaniesForm],
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)
  private val requireDomainData = new ActionRefiner[DataRequest, ResultsPageRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, ResultsPageRequiredParams[A]]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage),
          request.userAnswers.get(AssociatedCompaniesPage)
        ) match {
          case (
                Some(accPeriod),
                Some(taxableProfit),
                Some(distribution),
                maybeDistributionsIncluded,
                maybeAssociatedCompanies
              ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              ResultsPageRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                request,
                request.userId,
                request.userAnswers
              )
            )
          case _ => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      marginalReliefCalculatorConnector
        .calculate(
          request.accountingPeriod.accountingPeriodStartDate,
          request.accountingPeriod.accountingPeriodEndDateOrDefault,
          request.taxableProfit.toDouble,
          request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).map(_.toDouble),
          request.associatedCompanies.flatMap(_.associatedCompaniesCount),
          None,
          None
        )
        .map(calculatorResult =>
          Ok(
            view(
              calculatorResult,
              request.accountingPeriod,
              request.taxableProfit,
              request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).getOrElse(0),
              request.associatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0)
            )
          )
        )
    }
}
