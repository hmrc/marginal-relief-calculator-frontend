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
import models.{ Mode, UserAnswers }
import navigation.Navigator
import org.slf4j.LoggerFactory
import pages.InputScreenPage
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InputScreenView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class InputScreenController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: InputScreenFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InputScreenView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

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

            for {
              updatedAnswers <- Future.fromTry(request.userAnswers match {
                                  case Some(answers) =>
                                    answers.set(InputScreenPage, form)
                                  case None =>
                                    UserAnswers(request.userId).set(InputScreenPage, form)
                                })
              // validate updated user answers
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(InputScreenPage, mode, updatedAnswers)
            )
          }
        )
    }
}
