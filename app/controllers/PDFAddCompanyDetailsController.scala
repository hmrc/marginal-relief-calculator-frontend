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

import controllers.actions.{ DataRequiredAction, DataRetrievalAction, IdentifierAction, PDFRequiredDataAction }
import forms.PDFAddCompanyDetailsFormProvider
import models.NormalMode
import navigation.Navigator
import pages.PDFAddCompanyDetailsPage
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PDFAddCompanyDetailsView

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PDFAddCompanyDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  PDFRequiredDataAction: PDFRequiredDataAction,
  formProvider: PDFAddCompanyDetailsFormProvider,
  view: PDFAddCompanyDetailsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen PDFRequiredDataAction) {
    implicit request =>
      val preparedForm = request.userAnswers.get(PDFAddCompanyDetailsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, NormalMode))
  }
  def onSubmit: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen PDFRequiredDataAction).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, NormalMode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PDFAddCompanyDetailsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              nextPage       <- navigator.nextPage(PDFAddCompanyDetailsPage, NormalMode, updatedAnswers)
            } yield Redirect(nextPage)
        )
    }
}
