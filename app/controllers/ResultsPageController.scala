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
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ResultsPageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val maybeAccountingPeriodForm: Option[AccountingPeriodForm] = request.userAnswers.get(AccountingPeriodPage)
    val maybeTaxableProfit: Option[Int] = request.userAnswers.get(TaxableProfitPage)
    val maybeAssociatedCompanies: Option[AssociatedCompaniesForm] = request.userAnswers.get(AssociatedCompaniesPage)
    val maybeDistributionsIncludedForm: Option[DistributionsIncludedForm] =
      request.userAnswers.get(DistributionsIncludedPage)
    (maybeAccountingPeriodForm, maybeTaxableProfit) match {
      case (Some(accountingPeriodForm), Some(taxableProfit)) =>
        marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDate.get,
            taxableProfit.toDouble,
            maybeDistributionsIncludedForm.flatMap(_.distributionsIncludedAmount.map(_.toDouble)),
            maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount)
          )
          .map { marginalReliefResult =>
            Ok(
              view(
                marginalReliefResult,
                accountingPeriodForm,
                taxableProfit,
                maybeDistributionsIncludedForm.flatMap(_.distributionsIncludedAmount).getOrElse(0),
                maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0)
              )
            )
          }
      case _ =>
        throw new BadRequestException(
          "One or more user parameters required for calculation are missing. This could be either because the session has expired or " +
            "the user navigated directly to the results page. Missing parameters are [" + List(
              (AccountingPeriodPage, maybeAccountingPeriodForm),
              (TaxableProfitPage, maybeTaxableProfit)
            ).filter(_._2.isEmpty).map(_._1).mkString(",") + "]"
        )
    }
  }
}
