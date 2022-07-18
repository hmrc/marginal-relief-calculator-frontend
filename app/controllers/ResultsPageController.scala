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
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, InputScreenForm }
import org.slf4j.LoggerFactory
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, InputScreenPage, TaxableProfitPage }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ResultsPageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
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

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val maybeAccountingPeriodForm: Option[AccountingPeriodForm] = request.userAnswers.get(AccountingPeriodPage)
    val maybeTaxableProfit: Option[Int] = request.userAnswers.get(TaxableProfitPage)
    val maybeAssociatedCompanies: Option[AssociatedCompaniesForm] = request.userAnswers.get(AssociatedCompaniesPage)
    val maybeInputScreenForm: Option[InputScreenForm] = request.userAnswers.get(InputScreenPage)
    (maybeAccountingPeriodForm, maybeTaxableProfit, maybeInputScreenForm, maybeAssociatedCompanies) match {
      case (Some(accountingPeriodForm), Some(taxableProfit), Some(inputScreenForm), Some(associatedCompanies)) =>
        marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDate.get,
            taxableProfit.toDouble,
            Some(inputScreenForm.distribution),
            associatedCompanies.associatedCompaniesCount
          )
          .map { marginalReliefResult =>
            logger.info(s"received results: $marginalReliefResult")
            Ok(view(marginalReliefResult))
          }
      case _ =>
        throw new BadRequestException(
          "Some of the input parameters are missing. Missing parameters: " + List(
            (AccountingPeriodPage, maybeAccountingPeriodForm),
            (TaxableProfitPage, maybeTaxableProfit),
            (InputScreenPage, maybeInputScreenForm)
          ).filter(_._2.isEmpty).map(_._1)
        )
    }
  }
}
// $COVERAGE-ON$
