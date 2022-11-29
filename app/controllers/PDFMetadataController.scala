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
  formProvider: PDFMetadataFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PDFMetadataView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  case class PDFMetadataPageRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    associatedCompanies: Option[AssociatedCompaniesForm],
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)
  private val requireDomainData = new ActionRefiner[DataRequest, PDFMetadataPageRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, PDFMetadataPageRequiredParams[A]]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage),
          request.userAnswers.get(AssociatedCompaniesPage)
        ) match {
          case (
                Some(accPeriod),
                Some(taxableProfit),
                Some(distribution),
                maybeDistributionsIncluded,
                maybeAssociatedCompanies
              ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              PDFMetadataPageRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                request,
                request.userId,
                request.userAnswers
              )
            )
          case _ => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireDomainData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(PDFMetadataPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireDomainData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PDFMetadataPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              nextPage <- navigator.nextPage(PDFMetadataPage, NormalMode, updatedAnswers)
            } yield Redirect(nextPage)
        )
  }
}
