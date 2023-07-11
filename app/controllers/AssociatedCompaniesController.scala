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

import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel._
import controllers.actions._
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, AssociatedCompaniesFormProvider, DistributionsIncludedForm }
import models.requests.DataRequest
import models.{ AssociatedCompanies, Distribution, Mode, UserAnswers }
import navigation.Navigator
import org.slf4j.{ Logger, LoggerFactory }
import pages._
import play.api.data.Form
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AssociatedCompaniesView

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class AssociatedCompaniesController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AssociatedCompaniesFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AssociatedCompaniesView,
  marginalReliefCalculatorConnector: MarginalReliefCalculatorConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val form = formProvider()
  case class AssociatedCompaniesRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)
  private val requireDomainData = new ActionRefiner[DataRequest, AssociatedCompaniesRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, AssociatedCompaniesRequiredParams[A]]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage)
        ) match {
          case (Some(accPeriod), Some(taxableProfit), Some(distribution), maybeDistributionsIncluded)
              if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              AssociatedCompaniesRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
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
      for {
        associatedCompaniesParameter <- marginalReliefCalculatorConnector
                                          .associatedCompaniesParameters(
                                            request.accountingPeriod.accountingPeriodStartDate,
                                            request.accountingPeriod.accountingPeriodEndDateOrDefault
                                          )
        result <- ifAskAssociatedCompaniesThen(
                    associatedCompaniesParameter,
                    p =>
                      Future.successful(
                        Ok(
                          view(
                            request.userAnswers.get(AssociatedCompaniesPage).map(form.fill).getOrElse(form),
                            p,
                            mode
                          )
                        )
                      )
                  )
      } yield result
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      val boundedForm = form.bindFromRequest()
      for {
        associatedCompaniesParameter <- marginalReliefCalculatorConnector
                                          .associatedCompaniesParameters(
                                            request.accountingPeriod.accountingPeriodStartDate,
                                            request.accountingPeriod.accountingPeriodEndDateOrDefault
                                          )
        result <- boundedForm
                    .fold(
                      formWithErrors =>
                        ifAskAssociatedCompaniesThen(
                          associatedCompaniesParameter,
                          p => Future.successful(BadRequest(view(formWithErrors, p, mode)))
                        ),
                      value =>
                        validateRequiredFields(value, associatedCompaniesParameter) match {
                          case Some(errorKey) =>
                            ifAskAssociatedCompaniesThen(
                              associatedCompaniesParameter,
                              badRequestWithError(boundedForm, _, errorKey, mode)(request.request)
                            )
                          case None =>
                            updateAndRedirect(value, associatedCompaniesParameter, mode, request.userAnswers)
                        }
                    )
      } yield result
    }

  private def badRequestWithError(
    form: Form[AssociatedCompaniesForm],
    a: AskAssociatedCompaniesParameter,
    errorKey: String,
    mode: Mode
  )(implicit request: Request[AnyContent]) =
    Future.successful(
      BadRequest(
        view(
          form.withError(errorKey, "associatedCompaniesCount.error.required"),
          a,
          mode
        )
      )
    )

  private def updateAndRedirect(
    value: AssociatedCompaniesForm,
    associatedCompaniesParameter: AssociatedCompaniesParameter,
    mode: Mode,
    userAnswers: UserAnswers
  )(implicit headerCarrier: HeaderCarrier) =
    for {
      updated <- Future.fromTry(value.associatedCompanies match {
                   case AssociatedCompanies.Yes => userAnswers.set(AssociatedCompaniesPage, value)
                   case AssociatedCompanies.No =>
                     userAnswers
                       .set(
                         AssociatedCompaniesPage,
                         value.copy(
                           associatedCompaniesCount = None
                         )
                       )
                       .flatMap(_.remove(TwoAssociatedCompaniesPage))

                 })
      _        <- sessionRepository.set(updated)
      nextPage <- navigator.nextPage(AssociatedCompaniesPage, mode, updated)
    } yield Redirect(associatedCompaniesParameter match {
      case AskBothParts(_, _) if value.associatedCompanies == AssociatedCompanies.Yes =>
        routes.TwoAssociatedCompaniesController.onPageLoad(mode)
      case _ =>
        nextPage
    })

  private def validateRequiredFields(
    associatedCompaniesForm: AssociatedCompaniesForm,
    associatedCompaniesParameter: AssociatedCompaniesParameter
  ): Option[String] =
    associatedCompaniesParameter match {
      case AskFull | AskOnePart(_)
          if associatedCompaniesForm.associatedCompanies == AssociatedCompanies.Yes && associatedCompaniesForm.associatedCompaniesCount.isEmpty =>
        Some("associatedCompaniesCount")
      case _ =>
        None
    }

  private def ifAskAssociatedCompaniesThen(
    a: AssociatedCompaniesParameter,
    f: AskAssociatedCompaniesParameter => Future[Result]
  ): Future[Result] =
    a match {
      case DontAsk =>
        logger.info("Associated companies ask parameter is 'DontAsk'. Redirecting to CheckYourAnswers page")
        Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad().url))
      case p: AskAssociatedCompaniesParameter => f(p)
    }
}
