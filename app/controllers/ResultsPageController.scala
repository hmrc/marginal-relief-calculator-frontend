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
import models.ResultsPageData
import models.requests.DataRequest
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{ FullResultsPageView, ResultsPageView }

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class ResultsPageController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ResultsPageView,
  fullView: FullResultsPageView,
  marginalReliefCalculatorConnector: MarginalReliefCalculatorConnector
)(implicit val ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {
  private def getResultPageData(implicit request: DataRequest[_]): Future[Option[ResultsPageData]] = {
    val maybeAccountingPeriodForm = request.userAnswers.get(AccountingPeriodPage)
    val maybeTaxableProfit = request.userAnswers.get(TaxableProfitPage)
    val maybeAssociatedCompanies = request.userAnswers.get(AssociatedCompaniesPage)
    val maybeDistributionsIncludedForm = request.userAnswers.get(DistributionsIncludedPage)

    (maybeAccountingPeriodForm, maybeTaxableProfit) match {
      case (Some(accountingPeriodForm), Some(taxableProfit)) =>
        marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDate.get,
            taxableProfit.toDouble,
            maybeDistributionsIncludedForm.flatMap(
              _.distributionsIncludedAmount.map(_.toDouble)
            ),
            maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount)
          )
          .map(calculatorResult =>
            Some(
              ResultsPageData(
                accountingPeriodForm,
                taxableProfit,
                calculatorResult,
                maybeDistributionsIncludedForm.flatMap(_.distributionsIncludedAmount).getOrElse(0),
                maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0)
              )
            )
          )
      case _ => Future.successful(None)
    }

  }

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    getResultPageData.map {
      case Some(
            ResultsPageData(
              accountingPeriodForm,
              taxableProfit,
              calculatorResult,
              distributionsIncludedAmount,
              associatedCompaniesCount
            )
          ) =>
        Ok(
          view(
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributionsIncludedAmount,
            associatedCompaniesCount
          )
        )
      case None =>
        val maybeAccountingPeriodForm = request.userAnswers.get(AccountingPeriodPage)
        val maybeTaxableProfit = request.userAnswers.get(TaxableProfitPage)
        throw new BadRequestException(
          "One or more user parameters required for calculation are missing. This could be either because the session has expired or " +
            "the user navigated directly to the results page. Missing parameters are [" + List(
              (AccountingPeriodPage, maybeAccountingPeriodForm),
              (TaxableProfitPage, maybeTaxableProfit)
            ).filter(_._2.isEmpty).map(_._1).mkString(",") + "]"
        )
    }
  }

  def fullResultsOnPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      getResultPageData.map {
        case Some(
              ResultsPageData(
                accountingPeriodForm,
                taxableProfit,
                calculatorResult,
                distributionsIncludedAmount,
                associatedCompaniesCount
              )
            ) =>
          Ok(
            fullView(
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributionsIncludedAmount,
              associatedCompaniesCount
            )
          )
        case None =>
          val maybeAccountingPeriodForm = request.userAnswers.get(AccountingPeriodPage)
          val maybeTaxableProfit = request.userAnswers.get(TaxableProfitPage)
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
