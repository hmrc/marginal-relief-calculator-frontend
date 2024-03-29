/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions.{ DataRetrievalAction, IdentifierAction }
import models.{ NormalMode, UserAnswers }
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class IndexController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  view: IndexView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    Ok(view())
  }

  def onStart(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    for {
      updatedAnswers <- Future.successful(request.userAnswers match {
                          case Some(answers) =>
                            answers.clean
                          case None =>
                            UserAnswers(request.userId)
                        })
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      routes.AccountingPeriodController.onPageLoad(NormalMode).url
    )
  }
}
