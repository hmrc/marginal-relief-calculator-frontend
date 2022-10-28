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
import forms.{ AccountingPeriodForm, AccountingPeriodFormProvider }
import models.requests.OptionalDataRequest
import models.{ Mode, UserAnswers }
import navigation.Navigator
import org.slf4j.{ Logger, LoggerFactory }
import pages.AccountingPeriodPage
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, RequestHeader }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{ AccountingPeriodView, IrrelevantPeriodView }

import java.time.LocalDate
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
  view: AccountingPeriodView,
  irrelevantPeriodView: IrrelevantPeriodView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private def form(implicit req: RequestHeader) = formProvider(messagesApi.preferred(req))

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm =
      request.userAnswers.flatMap(_.get(AccountingPeriodPage)) match {
        case None => form
        case Some(value) =>
          form.fill(value.copy(accountingPeriodEndDate = Some(value.accountingPeriodEndDateOrDefault)))
      }
    Ok(view(preparedForm, mode))
  }

  private def updatedAnswers(request: OptionalDataRequest[AnyContent], form: AccountingPeriodForm) =
    Future.fromTry(request.userAnswers match {
      case Some(answers) =>
        answers
          .set(AccountingPeriodPage, form)
      case None =>
        UserAnswers(request.userId)
          .set(AccountingPeriodPage, form)
    })

  private def accountingPeriodIsIrrelevant(form: AccountingPeriodForm) =
    form.accountingPeriodStartDate.isBefore(LocalDate.parse("2022-04-02")) ||
      form.accountingPeriodEndDate.exists(_.isBefore(LocalDate.parse("2023-04-01")))

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          form =>
            if (accountingPeriodIsIrrelevant(form)) {
              logger.info("Accounting period is irrelevant as it lies before the beginning of the 2023 tax year")
              Future.successful(
                Redirect(
                  routes.AccountingPeriodController.irrelevantPeriodPage(mode)
                )
              )
            } else {
              for {
                updatedAnswers <- updatedAnswers(request, form)
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(AccountingPeriodPage, mode, updatedAnswers)
              )
            }
        )
    }

  def irrelevantPeriodPage(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    Ok(irrelevantPeriodView(mode))
  }
}
