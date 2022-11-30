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
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, PDFMetadataFormProvider }
import models.requests.DataRequest
import models.{ Distribution, NormalMode, UserAnswers }
import navigation.Navigator
import pages._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, ActionRefiner, AnyContent, MessagesControllerComponents, Request, Result, WrappedRequest }
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PDFMetadataView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class PDFMetadataController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  PDFRequiredDataAction: PDFRequiredDataAction,
  formProvider: PDFMetadataFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PDFMetadataView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen PDFRequiredDataAction) {
    implicit request =>
      val preparedForm = request.userAnswers.get(PDFMetadataPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen PDFRequiredDataAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PDFMetadataPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              nextPage       <- navigator.nextPage(PDFMetadataPage, NormalMode, updatedAnswers)
            } yield Redirect(nextPage)
        )
  }
}
