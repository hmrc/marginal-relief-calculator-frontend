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
import forms.{ AccountingPeriodForm, DistributionFormProvider }
import models.requests.DataRequest
import models.{ Distribution, Mode, UserAnswers }
import navigation.Navigator
import pages.{ AccountingPeriodPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DistributionView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class DistributionController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DistributionFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DistributionView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  case class DistributionRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)
  private val requireDomainData = new ActionRefiner[DataRequest, DistributionRequiredParams] {
    override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DistributionRequiredParams[A]]] =
      Future.successful {
        (request.userAnswers.get(AccountingPeriodPage), request.userAnswers.get(TaxableProfitPage)) match {
          case (Some(accPeriod), Some(taxableProfit)) =>
            Right(DistributionRequiredParams(accPeriod, taxableProfit, request, request.userId, request.userAnswers))
          case _ => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData) { implicit request =>
      val preparedForm = request.userAnswers.get(DistributionPage) match {
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
              updatedAnswers <-
                Future.fromTry(
                  request.userAnswers
                    .set(DistributionPage, value)
                    .flatMap(answer =>
                      if (value == Distribution.No) {
                        answer.remove(DistributionsIncludedPage)
                      } else {
                        Success(answer)
                      }
                    )
                )
              _        <- sessionRepository.set(updatedAnswers)
              nextPage <- navigator.nextPage(DistributionPage, mode, updatedAnswers)
            } yield Redirect(nextPage)
        )
    }
}
