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

import base.SpecBase
import models.associatedCompanies.{ AskBothParts, AskOnePart, DontAsk, Period }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, PDFAddCompanyDetailsForm, TwoAssociatedCompaniesForm }
import pages.*
import models.*
import play.api.inject.guice.GuiceApplicationBuilder
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import controllers.routes
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NavigatorSpec extends SpecBase with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val app: GuiceApplicationBuilder = applicationBuilder(None)
  val sessionRepository: SessionRepository = app.injector().instanceOf[SessionRepository]
  val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

  val navigator = new Navigator(mockParameterService, sessionRepository)

  "Navigator" - {

    "in Normal mode" - {
      "must go from AccountingPeriod page to TaxableProfit page" in {
        whenReady(navigator.nextPage(AccountingPeriodPage, NormalMode, UserAnswers("id"))) { result =>
          result mustBe routes.TaxableProfitController.onPageLoad(NormalMode)
        }

      }
      "must go from TaxableProfit page to Distribution page" in {
        whenReady(navigator.nextPage(TaxableProfitPage, NormalMode, UserAnswers("id"))) { result =>
          result mustBe routes.DistributionController.onPageLoad(NormalMode)
        }
      }

      "must go from Distribution to Distributions Included page" - {
        "With Distribution Page Yes" in {
          val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.Yes).success.value
          navigator.distributionsNextRoute(userAnswers) mustBe routes.DistributionsIncludedController.onPageLoad(
            NormalMode
          )

          whenReady(navigator.nextPage(DistributionPage, NormalMode, userAnswers)) { result =>
            result mustBe routes.DistributionsIncludedController.onPageLoad(NormalMode)
          }
        }

        "With Distribution Page No" in {
          val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.No).success.value
          navigator.distributionsNextRoute(userAnswers) mustBe routes.AssociatedCompaniesController.onPageLoad(
            NormalMode
          )
        }

        "With Distribution value not set" in {
          val userAnswers = UserAnswers("id")
          navigator.distributionsNextRoute(userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from Distribution Included page to Associated Companies page" in {
        whenReady(
          navigator.nextPage(
            DistributionsIncludedPage,
            NormalMode,
            UserAnswers("id")
          )
        ) { result =>
          result mustBe routes.AssociatedCompaniesController.onPageLoad(NormalMode)
        }
      }

      "must go from PDFAddCompanyDetailsPage to PDFMetaData page if answered yes" in {
        whenReady(
          navigator.nextPage(
            PDFAddCompanyDetailsPage,
            NormalMode,
            UserAnswers("id").set(PDFAddCompanyDetailsPage, PDFAddCompanyDetailsForm(PDFAddCompanyDetails.Yes)).get
          )
        ) { result =>
          result mustBe routes.PDFMetadataController.onPageLoad()
        }
      }

      "must go from PDFAddCompanyDetailsPage to PDFMetaData page if no answer present" in {
        whenReady(
          navigator.nextPage(
            PDFAddCompanyDetailsPage,
            NormalMode,
            UserAnswers("id")
          )
        ) { result =>
          result mustBe routes.PDFMetadataController.onPageLoad()
        }
      }

      "must go from PDFAddCompanyDetailsPage to Pdf page if answered no" in {
        whenReady(
          navigator.nextPage(
            PDFAddCompanyDetailsPage,
            NormalMode,
            UserAnswers("id").set(PDFAddCompanyDetailsPage, PDFAddCompanyDetailsForm(PDFAddCompanyDetails.No)).get
          )
        ) { result =>
          result mustBe routes.PDFController.onPageLoad()
        }
      }

      "must go from PDFMetadata page to PDF page" in {
        whenReady(
          navigator.nextPage(
            PDFMetadataPage,
            NormalMode,
            UserAnswers("id")
          )
        ) { result =>
          result mustBe routes.PDFController.onPageLoad()
        }
      }

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        whenReady(navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id"))) { result =>
          result mustBe routes.IndexController.onPageLoad()
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        whenReady(
          navigator.nextPage(
            UnknownPage,
            CheckMode,
            UserAnswers("id")
          )
        ) { result =>
          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from distribution to distributions included" in {
        val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.Yes).success.value
        whenReady(navigator.nextPage(DistributionPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.DistributionsIncludedController
            .onPageLoad(CheckMode)
        }
      }

      "must go from distribution to CheckYourAnswers" in {
        val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.No).success.value
        whenReady(navigator.nextPage(DistributionPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from distribution to JourneyRecoveryController when value not set" in {
        val userAnswers = UserAnswers("id")
        whenReady(navigator.nextPage(DistributionPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }

      "must go from AccountingPeriodPage to AssociatedCompaniesPage if two accounting companies are now required" in {
        val userAnswers = UserAnswers("id")
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.now(), None))
          .get
          .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1)))
          .get

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(
            Future.successful(
              AskBothParts(
                period1 = Period(start = LocalDate.now(), end = LocalDate.now()),
                period2 = Period(start = LocalDate.now(), end = LocalDate.now())
              )
            )
          )

        whenReady(navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.AssociatedCompaniesController.onPageLoad(CheckMode)
        }
      }

      "must go from AccountingPeriodPage to AssociatedCompaniesPage if associated companies are not asked" in {
        val userAnswers = UserAnswers("id")
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.now(), None))
          .get
          .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1)))
          .get

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(Future.successful(DontAsk))

        whenReady(navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from AccountingPeriodPage to CheckYourAnswersPage if type of associated companies did not change" in {
        val userAnswers = UserAnswers("id")
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.now(), None))
          .get
          .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1)))
          .get

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(
            Future.successful(
              result = AskOnePart(period = Period(start = LocalDate.now(), end = LocalDate.now()))
            )
          )

        whenReady(navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from AccountingPeriodPage to AssociatedCompaniesPage if two associated companies changed to one" in {
        val userAnswers = UserAnswers("id")
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.now(), None))
          .get
          .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, None))
          .get
          .set(TwoAssociatedCompaniesPage, TwoAssociatedCompaniesForm(Some(1), Some(1)))
          .get

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(
            Future.successful(
              result = AskOnePart(period = Period(start = LocalDate.now(), end = LocalDate.now()))
            )
          )

        whenReady(navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.AssociatedCompaniesController.onPageLoad(CheckMode)
        }
      }

      "must go from AccountingPeriodPage to CheckYourAnswersPage if type of two associated companies did not change" in {
        val userAnswers = UserAnswers("id")
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.now(), None))
          .get
          .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, None))
          .get
          .set(TwoAssociatedCompaniesPage, TwoAssociatedCompaniesForm(Some(1), Some(1)))
          .get

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(
            Future.successful(
              result = AskBothParts(
                period1 = Period(start = LocalDate.now(), end = LocalDate.now()),
                period2 = Period(start = LocalDate.now(), end = LocalDate.now())
              )
            )
          )

        whenReady(navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)) { result =>
          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must return error when navigating to next page on AccountingPeriodPage if periods not present" in {
        val userAnswers = UserAnswers("id")

        when(
          mockParameterService
            .associatedCompaniesParameters(
              accountingPeriodStart = any[LocalDate],
              accountingPeriodEnd = any[LocalDate]
            )
        )
          .thenReturn(
            Future.successful(
              result = AskOnePart(period = Period(start = LocalDate.now(), end = LocalDate.now()))
            )
          )

        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            navigator.nextPage(AccountingPeriodPage, CheckMode, userAnswers)
          }
        caught.getMessage mustBe "Accounting period data is not available"
      }
    }
  }
}
