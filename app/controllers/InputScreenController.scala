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
import forms.InputScreenFormProvider
import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.InputScreenPage
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InputScreenView

import scala.concurrent.{ ExecutionContext, Future }

import models.UserAnswers

class InputScreenController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InputScreenFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InputScreenView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm =
      request.userAnswers.flatMap(_.get(InputScreenPage)) match {
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
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers match {
                                  case Some(answers) =>
                                    answers.set(InputScreenPage, value)
                                  case None =>
                                    UserAnswers(request.userId).set(InputScreenPage, value)
                                })
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(InputScreenPage, mode, updatedAnswers)
            )
        )
    }
}
