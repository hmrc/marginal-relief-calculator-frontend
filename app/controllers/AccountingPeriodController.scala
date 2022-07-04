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

import controllers.actions._
import forms.DateUtils.DateOps
import forms.{ AccountingPeriodForm, AccountingPeriodFormProvider }
import models.{ Mode, UserAnswers }
import navigation.Navigator
import pages.AccountingPeriodPage
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AccountingPeriodView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class AccountingPeriodController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: AccountingPeriodFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AccountingPeriodView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  def form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm =
      request.userAnswers.flatMap(_.get(AccountingPeriodPage)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

    Ok(view(preparedForm, mode))
  }

  private def validateForm(form: Form[AccountingPeriodForm]) =
    form.value match {
      case Some(accountingPeriodForm)
          if accountingPeriodForm.accountingPeriodEndDate.isDefined && accountingPeriodForm.accountingPeriodEndDate.get
            .isEqualOrBefore(
              accountingPeriodForm.accountingPeriodStartDate
            ) =>
        form.withError("accountingPeriodEndDate.day", "accountingPeriod.error.startShouldBeBeforeEnd")
      case _ => form
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          form => {
            val formWithErrors = validateForm(form)
            logger.error(
              s"Failed to bind request for marginal relief calculation [errors=${formWithErrors.errors.map(_.message)}]"
            )
            Future.successful(BadRequest(view(validateForm(formWithErrors), mode)))
          },
          form => {
            logger.info(
              s"Received request for marginal relief calculation [data=${request.userAnswers.map(_.data.toString()).getOrElse("")}]"
            )

            // setting defaults for missing fields
            val formWithAccountingPeriodEnd = form.copy(accountingPeriodEndDate =
              form.accountingPeriodEndDate.orElse(Some(form.accountingPeriodStartDate.plusYears(1).minusDays(1)))
            )

            for {
              updatedAnswers <- Future.fromTry(request.userAnswers match {
                                  case Some(answers) =>
                                    answers.set(AccountingPeriodPage, formWithAccountingPeriodEnd)
                                  case None =>
                                    UserAnswers(request.userId).set(AccountingPeriodPage, formWithAccountingPeriodEnd)
                                })
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(AccountingPeriodPage, mode, updatedAnswers)
            )
          }
        )
    }
}
