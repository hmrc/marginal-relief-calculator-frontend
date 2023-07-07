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

import controllers.actions.IdentifierAction
import models.requests.IdentifierRequest
import org.slf4j.LoggerFactory
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyRecoveryController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  continueView: JourneyRecoveryContinueView,
  startAgainView: JourneyRecoveryStartAgainView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def onPageLoad(continueUrl: Option[RedirectUrl] = None): Action[AnyContent] = identify.async { implicit request =>
    continueUrl match {
      case Some(unsafeUrl) =>
        unsafeUrl.getEither(OnlyRelative) match {
          case Right(safeUrl) =>
            Future.successful(Ok(continueView(safeUrl.url)))
          case Left(message) =>
            logger.info(message)
            clearSessionAndStartView()
        }
      case None =>
        clearSessionAndStartView()
    }
  }

  private def clearSessionAndStartView()(implicit request: IdentifierRequest[AnyContent]) = for {
    _ <- sessionRepository.clear(request.userId)
  } yield Ok(startAgainView())
}
