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
import forms.AccountingPeriodFormProvider
import models.{Mode, UserAnswers}

import javax.inject.Inject
import navigation.Navigator
import pages.{AccountingPeriodPage, InputScreenPage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AccountingPeriodView

import scala.concurrent.{ExecutionContext, Future}

class AccountingPeriodController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: AccountingPeriodFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: AccountingPeriodView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm =
      request.userAnswers.flatMap(_.get(AccountingPeriodPage)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

    Ok(view(preparedForm, mode))
  }


  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            logger.error(
              s"Failed to bind request for marginal relief calculation [errors=${formWithErrors.errors.map(_.message)}]"
            )
            Future.successful(BadRequest(view(formWithErrors, mode)))
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
              // validate updated user answers
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(AccountingPeriodPage, mode, updatedAnswers)
            )
          }
        )
    }
}