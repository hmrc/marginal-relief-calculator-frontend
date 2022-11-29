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

import com.google.inject.Inject
import controllers.actions._
import forms.{ AccountingPeriodForm, TaxableProfitFormProvider }
import models.requests.DataRequest
import models.{ Mode, UserAnswers }
import navigation.Navigator
import pages.{ AccountingPeriodPage, TaxableProfitPage }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TaxableProfitView

import scala.concurrent.{ ExecutionContext, Future }

class TaxableProfitController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TaxableProfitFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TaxableProfitView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  case class TaxableProfitRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)
  private val requireDomainData = new ActionRefiner[DataRequest, TaxableProfitRequiredParams] {
    override protected def refine[A](request: DataRequest[A]): Future[Either[Result, TaxableProfitRequiredParams[A]]] =
      Future.successful {
        request.userAnswers.get(AccountingPeriodPage) match {
          case None        => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Some(value) => Right(TaxableProfitRequiredParams(value, request, request.userId, request.userAnswers))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData) { implicit request =>
      val preparedForm = request.userAnswers.get(TaxableProfitPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TaxableProfitPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              nextPage <- navigator.nextPage(TaxableProfitPage, mode, updatedAnswers)
            } yield Redirect(nextPage)
        )
    }
}
