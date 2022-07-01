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
import forms.{ AccountingPeriodForm, InputScreenForm }
import org.slf4j.LoggerFactory
import pages.{ AccountingPeriodPage, InputScreenPage }

import javax.inject.Inject
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ResultsPageView

import scala.concurrent.{ ExecutionContext, Future }

class ResultsPageController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ResultsPageView,
  marginalReliefCalculatorConnector: MarginalReliefCalculatorConnector[Future]
)(implicit val ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val maybeAccountingPeriodForm: Option[AccountingPeriodForm] = request.userAnswers.get(AccountingPeriodPage)
    val maybeInputScreenForm: Option[InputScreenForm] = request.userAnswers.get(InputScreenPage)
    (maybeAccountingPeriodForm, maybeInputScreenForm) match {
      case (Some(accountingPeriodForm), Some(inputScreenForm)) =>
        marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDate.get,
            inputScreenForm.profit,
            Some(inputScreenForm.distribution),
            Some(inputScreenForm.associatedCompanies)
          )
          .map { marginalReliefResult =>
            logger.info(s"received results: $marginalReliefResult")
            Ok(view(marginalReliefResult))
          }
      case (Some(_), None) => throw new BadRequestException("input screen parameters not provided")
      case (None, Some(_)) => throw new BadRequestException("accounting period not provided")
      case (None, None)    => throw new BadRequestException("Calculation parameters not provided")
    }
  }
}
