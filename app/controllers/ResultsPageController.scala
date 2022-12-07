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
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, TwoAssociatedCompaniesForm }
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
    twoAssociatedCompanies: Option[TwoAssociatedCompaniesForm],
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
          request.userAnswers.get(AssociatedCompaniesPage),
          request.userAnswers.get(TwoAssociatedCompaniesPage)
        ) match {
          case (
                Some(accPeriod),
                Some(taxableProfit),
                Some(distribution),
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                maybeTwoAssociatedCompanies
              ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              ResultsPageRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                maybeTwoAssociatedCompanies,
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
      val associatedCompanies = request.twoAssociatedCompanies match {
        case Some(a) =>
          Right(a.associatedCompaniesFY1Count.getOrElse(0), a.associatedCompaniesFY2Count.getOrElse(0))
        case None => Left(request.associatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0))
      }
      marginalReliefCalculatorConnector
        .calculate(
          request.accountingPeriod.accountingPeriodStartDate,
          request.accountingPeriod.accountingPeriodEndDateOrDefault,
          request.taxableProfit.toDouble,
          request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).map(_.toDouble),
          request.associatedCompanies.flatMap(_.associatedCompaniesCount),
          request.twoAssociatedCompanies.flatMap(_.associatedCompaniesFY1Count),
          request.twoAssociatedCompanies.flatMap(_.associatedCompaniesFY2Count)
        )
        .map(calculatorResult =>
          Ok(
            view(
              calculatorResult,
              request.accountingPeriod,
              request.taxableProfit,
              request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).getOrElse(0),
              associatedCompanies
            )
          )
        )
    }
}
