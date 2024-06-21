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

package navigation

import com.google.inject.{ Inject, Singleton }
import models.associatedCompanies.{ AskBothParts, AskFull, AskOnePart, DontAsk }
import controllers.routes
import forms.TwoAssociatedCompaniesForm
import models._
import pages._
import play.api.mvc.Call
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class Navigator @Inject() (
  associatedCompaniesParameterService: AssociatedCompaniesParameterService,
  sessionRepository: SessionRepository
)(implicit
  executionContext: ExecutionContext
) {

  private val normalRoutes: Page => UserAnswers => Call = {
    case AccountingPeriodPage =>
      _ => routes.TaxableProfitController.onPageLoad(NormalMode)

    case TaxableProfitPage =>
      _ => routes.DistributionController.onPageLoad(NormalMode)

    case DistributionPage => distributionsNextRoute

    case DistributionsIncludedPage =>
      _ => routes.AssociatedCompaniesController.onPageLoad(NormalMode)

    case AssociatedCompaniesPage =>
      _ => routes.CheckYourAnswersController.onPageLoad()

    case PDFMetadataPage =>
      _ => routes.PDFController.onPageLoad()

    case TwoAssociatedCompaniesPage =>
      _ => routes.CheckYourAnswersController.onPageLoad()

    case PDFAddCompanyDetailsPage =>
      answers =>
        val details = answers
          .get(PDFAddCompanyDetailsPage)
          .map(details => details.pdfAddCompanyDetails)
          .getOrElse(PDFAddCompanyDetails.Yes)
        details match {
          case PDFAddCompanyDetails.Yes =>
            routes.PDFMetadataController.onPageLoad()
          case PDFAddCompanyDetails.No =>
            routes.PDFController.onPageLoad()

        }

    case _ =>
      _ => routes.IndexController.onPageLoad()
  }

  private def checkRouteMap: Page => UserAnswers => Future[Call] = {
    case AccountingPeriodPage => accountingPeriodChangeRoute
    case DistributionPage     => userAnswers => Future.successful(distributionsChangeRoute(userAnswers))
    case _                    => _ => Future.successful(routes.CheckYourAnswersController.onPageLoad())
  }

  private def accountingPeriodChangeRoute(answers: UserAnswers): Future[Call] = {
    val needToProcessNextPage =
      answers.get(AssociatedCompaniesPage).forall(_.associatedCompanies == AssociatedCompanies.Yes)

    def processNextPage = answers
      .get(AccountingPeriodPage)
      .map { accountingPeriodForm =>
        associatedCompaniesParameterService
          .associatedCompaniesParameters(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDateOrDefault
          )
          .flatMap {
            case DontAsk =>
              resetAssociatedCompanies(answers).map(_ => routes.CheckYourAnswersController.onPageLoad())
            case AskBothParts(period1, period2) =>
              val twoAssociatedCompaniesExist =
                answers.get(TwoAssociatedCompaniesPage).exists { case TwoAssociatedCompaniesForm(one, two) =>
                  one.nonEmpty && two.nonEmpty
                }
              if (twoAssociatedCompaniesExist) {
                Future.successful(routes.CheckYourAnswersController.onPageLoad())
              } else {
                resetAssociatedCompanies(answers).map { _ =>
                  routes.AssociatedCompaniesController.onPageLoad(CheckMode)
                }
              }
            case AskFull | AskOnePart(_) =>
              val onlyOneAssociatedCompanyExists =
                answers.get(AssociatedCompaniesPage).exists(_.associatedCompaniesCount.nonEmpty)
              if (onlyOneAssociatedCompanyExists) {
                Future.successful(routes.CheckYourAnswersController.onPageLoad())
              } else {
                resetAssociatedCompanies(answers).map { _ =>
                  routes.AssociatedCompaniesController.onPageLoad(CheckMode)
                }
              }
          }
      }
      .getOrElse(throw new RuntimeException("Accounting period data is not available"))

    if (needToProcessNextPage) {
      processNextPage
    } else {
      Future.successful(routes.CheckYourAnswersController.onPageLoad())
    }
  }

  private def resetAssociatedCompanies(answers: UserAnswers): Future[Boolean] =
    Future
      .fromTry(for {
        x1 <- answers.remove(TwoAssociatedCompaniesPage)
        x2 <- x1.remove(AssociatedCompaniesPage)
      } yield sessionRepository.set(x2))
      .flatten

  def distributionsNextRoute(answers: UserAnswers): Call =
    answers.get(DistributionPage) match {
      case Some(Distribution.Yes) => routes.DistributionsIncludedController.onPageLoad(NormalMode)
      case Some(Distribution.No)  => routes.AssociatedCompaniesController.onPageLoad(NormalMode)
      case _                      => routes.JourneyRecoveryController.onPageLoad()
    }

  def distributionsChangeRoute(answers: UserAnswers): Call =
    answers.get(DistributionPage) match {
      case Some(Distribution.Yes) => routes.DistributionsIncludedController.onPageLoad(CheckMode)
      case Some(Distribution.No)  => routes.CheckYourAnswersController.onPageLoad()
      case _                      => routes.JourneyRecoveryController.onPageLoad()
    }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call] =
    mode match {
      case NormalMode =>
        Future.successful(normalRoutes(page)(userAnswers))
      case CheckMode =>
        checkRouteMap(page)(userAnswers)
    }
}
