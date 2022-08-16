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
import models.{ CheckMode, Mode, NormalMode, UserAnswers }
import navigation.Navigator
import org.slf4j.{ Logger, LoggerFactory }
import pages.AccountingPeriodPage
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
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

  private def form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm =
      request.userAnswers.flatMap(_.get(AccountingPeriodPage)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

    Ok(view(preparedForm, mode))
  }

  private def updatedAnswersAndMode(request: OptionalDataRequest[AnyContent], form: AccountingPeriodForm, mode: Mode) =
    Future.fromTry(request.userAnswers match {
      case Some(answers) if mode == CheckMode =>
        answers
          .set(AccountingPeriodPage, form)
          .map(_ -> {
            answers
              .get(AccountingPeriodPage)
              .collect {
                case prevAnswer if prevAnswer == form =>
                  CheckMode
                case _ =>
                  NormalMode
              }
              .getOrElse(mode)
          })
      case Some(answers) =>
        answers
          .set(AccountingPeriodPage, form)
          .map(_ -> mode)
      case None =>
        UserAnswers(request.userId)
          .set(AccountingPeriodPage, form)
          .map(_ -> mode)
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
          form => {
            // setting defaults for missing fields
            val formWithAccountingPeriodEnd = form.copy(accountingPeriodEndDate = form.accountingPeriodEndDate.orElse {
              val endDate = form.accountingPeriodStartDate.plusYears(1).minusDays(1)
              logger.info(s"End date not provided, setting default end date to $endDate")
              Some(endDate)
            })

            if (accountingPeriodIsIrrelevant(formWithAccountingPeriodEnd)) {
              logger.info("Accounting period is irrelevant as it lies before the beginning of the 2023 tax year")
              Future.successful(
                Redirect(
                  routes.AccountingPeriodController.irrelevantPeriodPage()
                )
              )
            } else {
              for {
                (updatedAnswers, mode) <- updatedAnswersAndMode(request, formWithAccountingPeriodEnd, mode)
                _                      <- sessionRepository.set(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(AccountingPeriodPage, mode, updatedAnswers)
              )
            }
          }
        )
    }

  def irrelevantPeriodPage(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    Ok(irrelevantPeriodView())
  }
}
