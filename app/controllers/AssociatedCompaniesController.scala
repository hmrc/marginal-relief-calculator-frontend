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

import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel._
import controllers.actions._
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, AssociatedCompaniesFormProvider }
import models.requests.DataRequest
import models.{ AssociatedCompanies, Mode }
import navigation.Navigator
import org.slf4j.{ Logger, LoggerFactory }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, TaxableProfitPage }
import play.api.data.Form
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import repositories.SessionRepository
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

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      for {
        userParameters <- getUserParameters
        associatedCompaniesParameter <- marginalReliefCalculatorConnector
                                          .associatedCompaniesParameters(
                                            userParameters.accountingPeriodForm.accountingPeriodStartDate,
                                            userParameters.accountingPeriodForm.accountingPeriodEndDate.get,
                                            userParameters.taxableProfit,
                                            None
                                          )
      } yield ifAskAssociatedCompaniesThen(
        associatedCompaniesParameter,
        p =>
          Ok(
            view(
              request.userAnswers.get(AssociatedCompaniesPage).map(form.fill).getOrElse(form),
              p,
              mode
            )
          )
      )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val boundedForm = form.bindFromRequest()
      for {
        userParameters <- getUserParameters
        associatedCompaniesParameter <- marginalReliefCalculatorConnector
                                          .associatedCompaniesParameters(
                                            userParameters.accountingPeriodForm.accountingPeriodStartDate,
                                            userParameters.accountingPeriodForm.accountingPeriodEndDate.get,
                                            userParameters.taxableProfit,
                                            None
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
                              badRequestWithError(boundedForm, _, errorKey, mode)
                            )
                          case None =>
                            updateAndRedirect(value, mode)
                        }
                    )
      } yield result
  }

  private def badRequestWithError(
    form: Form[AssociatedCompaniesForm],
    a: AskAssociatedCompaniesParameter,
    errorKey: String,
    mode: Mode
  )(implicit request: DataRequest[AnyContent]) =
    Future.successful(
      BadRequest(
        view(
          form.withError(errorKey, "associatedCompaniesCount.error.required"),
          a,
          mode
        )
      )
    )

  private def updateAndRedirect(value: AssociatedCompaniesForm, mode: Mode)(implicit
    request: DataRequest[AnyContent]
  ) = {
    val valueUpdated = value.associatedCompanies match {
      case AssociatedCompanies.Yes => value
      case AssociatedCompanies.No =>
        value.copy(
          associatedCompaniesCount = None,
          associatedCompaniesFY1Count = None,
          associatedCompaniesFY2Count = None
        )
    }
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AssociatedCompaniesPage, valueUpdated))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(AssociatedCompaniesPage, mode, updatedAnswers))
  }

  case class AccountingPeriodTaxableProfit(accountingPeriodForm: AccountingPeriodForm, taxableProfit: Int)
  private def getUserParameters()(implicit
    request: DataRequest[AnyContent]
  ): Future[AccountingPeriodTaxableProfit] = {
    val userAnswers = request.userAnswers
    (userAnswers.get(AccountingPeriodPage), userAnswers.get(TaxableProfitPage)) match {
      case (Some(accountingPeriodForm), Some(taxableProfit)) =>
        Future.successful(AccountingPeriodTaxableProfit(accountingPeriodForm, taxableProfit))
      case _ =>
        logger.error("Missing values for AccountingPeriodPage and(or) TaxableProfitPage")
        Future.failed(new RuntimeException("Missing values for AccountingPeriodPage and(or) TaxableProfitPage"))
    }
  }

  private def validateRequiredFields(
    associatedCompaniesForm: AssociatedCompaniesForm,
    associatedCompaniesParameter: AssociatedCompaniesParameter
  ): Option[String] =
    associatedCompaniesParameter match {
      case AskFull | AskOnePart(_)
          if associatedCompaniesForm.associatedCompanies == AssociatedCompanies.Yes && associatedCompaniesForm.associatedCompaniesCount.isEmpty =>
        Some("associatedCompaniesCount")
      case AskBothParts(_, _)
          if associatedCompaniesForm.associatedCompanies == AssociatedCompanies.Yes && associatedCompaniesForm.associatedCompaniesFY1Count.isEmpty =>
        Some("associatedCompaniesFY1Count")
      case AskBothParts(_, _)
          if associatedCompaniesForm.associatedCompanies == AssociatedCompanies.Yes && associatedCompaniesForm.associatedCompaniesFY2Count.isEmpty =>
        Some("associatedCompaniesFY2Count")
      case _ =>
        None
    }

  private def ifAskAssociatedCompaniesThen[T](
    a: AssociatedCompaniesParameter,
    f: AskAssociatedCompaniesParameter => T
  ): T =
    a match {
      case DontAsk =>
        throw new UnsupportedOperationException("Associated companies ask parameter value is 'DontAsk'")
      case p: AskAssociatedCompaniesParameter => f(p)
    }
}
