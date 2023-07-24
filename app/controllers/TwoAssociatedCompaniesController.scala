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

import connectors.sharedmodel.{AskBothParts, AssociatedCompaniesParameter}
import controllers.actions._
import forms.DateUtils.financialYear
import forms._
import models.requests.DataRequest
import models.{Distribution, Mode, UserAnswers}
import navigation.Navigator
import pages._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import providers.AssociatedCompaniesParametersProvider
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TwoAssociatedCompaniesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TwoAssociatedCompaniesController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TwoAssociatedCompaniesFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TwoAssociatedCompaniesView,
  associatedCompaniesParametersProvider: AssociatedCompaniesParametersProvider
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  case class TwoAssociatedCompaniesRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    associatedCompaniesForm: AssociatedCompaniesForm,
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)

  private val requireDomainData = new ActionRefiner[DataRequest, TwoAssociatedCompaniesRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, TwoAssociatedCompaniesRequiredParams[A]]] =
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
                Some(associatedCompaniesForm)
              ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              TwoAssociatedCompaniesRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                associatedCompaniesForm,
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

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      val form = getForm
      for {
        associatedCompaniesParameter <- associatedCompaniesParametersProvider
                                          .associatedCompaniesParameters(
                                            request.accountingPeriod.accountingPeriodStartDate,
                                            request.accountingPeriod.accountingPeriodEndDateOrDefault
                                          )
        result <-
          ifAskAssocAskBothPartsThen(
            associatedCompaniesParameter,
            askBothParts => {
              val preparedForm = request.userAnswers.get(TwoAssociatedCompaniesPage) match {
                case None        => form
                case Some(value) => form.fill(value)
              }
              Future.successful(Ok(view(preparedForm, request.accountingPeriod, askBothParts, mode)))
            }
          )
      } yield result
    }

  private def validateRequiredFields(
    twoAssociatedCompaniesForm: TwoAssociatedCompaniesForm
  ): Option[String] =
    if (
      twoAssociatedCompaniesForm.associatedCompaniesFY1Count.isEmpty && twoAssociatedCompaniesForm.associatedCompaniesFY2Count.isEmpty
    ) {
      Some("twoAssociatedCompanies.error.enterAtLeastOneAnswer")
    } else if (
      (twoAssociatedCompaniesForm.associatedCompaniesFY1Count.contains(
        0
      ) && twoAssociatedCompaniesForm.associatedCompaniesFY2Count.contains(0)) ||
      (twoAssociatedCompaniesForm.associatedCompaniesFY1Count.isEmpty && twoAssociatedCompaniesForm.associatedCompaniesFY2Count
        .contains(0)) ||
      (twoAssociatedCompaniesForm.associatedCompaniesFY1Count.contains(
        0
      ) && twoAssociatedCompaniesForm.associatedCompaniesFY2Count.isEmpty)
    ) {
      Some("twoAssociatedCompanies.error.enterAtLeastOneValueGreaterThan0")
    } else {
      None
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      val boundedForm = getForm
        .bindFromRequest()
      boundedForm
        .fold(
          formWithErrors =>
            for {
              associatedCompaniesParameter <- associatedCompaniesParametersProvider
                                                .associatedCompaniesParameters(
                                                  request.accountingPeriod.accountingPeriodStartDate,
                                                  request.accountingPeriod.accountingPeriodEndDateOrDefault
                                                )
              result <-
                ifAskAssocAskBothPartsThen(
                  associatedCompaniesParameter,
                  askBothParts =>
                    Future.successful(BadRequest(view(formWithErrors, request.accountingPeriod, askBothParts, mode)))
                )
            } yield result,
          value =>
            validateRequiredFields(value) match {
              case Some(errorKey) =>
                for {
                  associatedCompaniesParameter <- associatedCompaniesParametersProvider
                                                    .associatedCompaniesParameters(
                                                      request.accountingPeriod.accountingPeriodStartDate,
                                                      request.accountingPeriod.accountingPeriodEndDateOrDefault
                                                    )
                  result <-
                    ifAskAssocAskBothPartsThen(
                      associatedCompaniesParameter,
                      askBothParts =>
                        badRequestWithError(request.accountingPeriod, boundedForm, askBothParts, errorKey, mode)
                    )
                } yield result
              case None =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(TwoAssociatedCompaniesPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                  nextPage       <- navigator.nextPage(TwoAssociatedCompaniesPage, mode, updatedAnswers)
                } yield Redirect(nextPage)
            }
        )
    }

  private def ifAskAssocAskBothPartsThen(
    a: AssociatedCompaniesParameter,
    f: AskBothParts => Future[Result]
  ): Future[Result] =
    a match {
      case b: AskBothParts =>
        f(b)
      case _ =>
        throw new UnsupportedOperationException(
          "Two associated companies page should only be requested when associated companies is required for both the notional accounting periods"
        )
    }

  private def getForm(implicit request: TwoAssociatedCompaniesRequiredParams[AnyContent]) =
    formProvider(
      financialYear(request.accountingPeriod.accountingPeriodStartDate),
      financialYear(request.accountingPeriod.accountingPeriodEndDateOrDefault)
    )
  private def badRequestWithError(
    accountingPeriod: AccountingPeriodForm,
    form: Form[TwoAssociatedCompaniesForm],
    askBothParts: AskBothParts,
    errorMessage: String,
    mode: Mode
  )(implicit request: Request[AnyContent]) =
    Future.successful(
      BadRequest(
        view(
          form.withError("associatedCompaniesFY1Count", errorMessage),
          accountingPeriod,
          askBothParts,
          mode
        )
      )
    )
}
