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

import org.apache.pekko.stream.Materializer
import base.SpecBase
import models.MarginalReliefConfig
import models.calculator.{ DualResult, FYRatio, MarginalRate, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, PDFAddCompanyDetailsForm, PDFMetadataForm, TwoAssociatedCompaniesForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, PDFAddCompanyDetails }
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.mockito.MockitoSugar
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, PDFAddCompanyDetailsPage, PDFMetadataPage, TaxableProfitPage, TwoAssociatedCompaniesPage }
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ CalculationConfigService, CalculatorService }
import utils.{ DateTime, FakeDateTime }
import views.html.PDFView

import java.time.{ LocalDate, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class PDFControllerSpec extends SpecBase with MockitoSugar {

  private val fakeDateTime = new FakeDateTime()
  private lazy val pdfViewRoute = routes.PDFController.onPageLoad().url
  private lazy val pdfSaveRoute = routes.PDFController.downloadPdf().url

  private val config = Map(
    2023 -> MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
  )
  private val accountingPeriodForm =
    AccountingPeriodForm(LocalDate.parse("2023-04-01"), Some(LocalDate.parse("2024-03-31")))
  private val distributionsIncludedForm = DistributionsIncludedForm(
    DistributionsIncluded.Yes,
    Some(1)
  )
  private val associatedCompaniesForm = AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))
  private val utrString = "1234567890"
  private val pdfMetadataForm = Option(PDFMetadataForm(Some("company"), Some(utrString)))

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, accountingPeriodForm)
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.Yes)
    .get
    .set(
      DistributionsIncludedPage,
      distributionsIncludedForm
    )
    .get
    .set(
      AssociatedCompaniesPage,
      associatedCompaniesForm
    )
    .get
    .set(
      PDFMetadataPage,
      PDFMetadataForm(Some("company"), Some(utrString))
    )
    .get
    .set(
      PDFAddCompanyDetailsPage,
      PDFAddCompanyDetailsForm(
        PDFAddCompanyDetails.Yes
      )
    )
    .get

  "PDFController" - {
    "GET /pdf" - {
      "must render pdf page when data is available for single year" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(
            year = accountingPeriodForm.accountingPeriodStartDate.getYear,
            corporationTaxBeforeMR = 1,
            taxRateBeforeMR = 1,
            corporationTax = 1,
            taxRate = 1,
            marginalRelief = 1,
            adjustedProfit = 1,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1,
            adjustedLowerThreshold = 1,
            adjustedUpperThreshold = 1,
            days = 1,
            fyRatio = FYRatio(1, 365)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) thenReturn Future.successful(Map(2023 -> config(2023)))

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            profit = 1,
            exemptDistributions = Some(1),
            associatedCompanies = Some(1),
            associatedCompaniesFY1 = None,
            associatedCompaniesFY2 = None
          )
        ) thenReturn Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PDFView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(
              pdfMetadata = pdfMetadataForm,
              calculatorResult = calculatorResult,
              accountingPeriodForm = accountingPeriodForm,
              taxableProfit = 1,
              distributions = 1,
              associatedCompanies = Left(1),
              config = config,
              currentInstant = fakeDateTime.currentInstant,
              request = request,
              messages = messages(application)
            )
            .toString
            .filterAndTrim
        }
      }

      "must render redirect to recovery controller when data is not available" in {
        val mockCalculatorService: CalculatorService =
          mock[CalculatorService]
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          header(HeaderNames.LOCATION, result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
        }
      }

      "must render pdf page when data is available for dual year" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val dualYearAnswers = emptyUserAnswers
          .set(AccountingPeriodPage, accountingPeriodForm)
          .get
          .set(TaxableProfitPage, 1)
          .get
          .set(DistributionPage, Distribution.Yes)
          .get
          .set(
            DistributionsIncludedPage,
            distributionsIncludedForm
          )
          .get
          .set(
            PDFMetadataPage,
            PDFMetadataForm(Some("company"), Some(utrString))
          )
          .get
          .set(
            page = TwoAssociatedCompaniesPage,
            value = TwoAssociatedCompaniesForm(
              associatedCompaniesFY1Count = Some(1),
              associatedCompaniesFY2Count = Some(2)
            )
          )
          .get

        val application = applicationBuilder(userAnswers = Some(dualYearAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = DualResult(
          MarginalRate(
            year = accountingPeriodForm.accountingPeriodStartDate.getYear,
            corporationTaxBeforeMR = 1,
            taxRateBeforeMR = 1,
            corporationTax = 1,
            taxRate = 1,
            marginalRelief = 1,
            adjustedProfit = 1,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1,
            adjustedLowerThreshold = 1,
            adjustedUpperThreshold = 1,
            days = 1,
            fyRatio = FYRatio(1, 365)
          ),
          MarginalRate(
            year = accountingPeriodForm.accountingPeriodStartDate.getYear,
            corporationTaxBeforeMR = 1,
            taxRateBeforeMR = 1,
            corporationTax = 1,
            taxRate = 1,
            marginalRelief = 1,
            adjustedProfit = 1,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1,
            adjustedLowerThreshold = 1,
            adjustedUpperThreshold = 1,
            days = 1,
            fyRatio = FYRatio(1, 365)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) thenReturn Future.successful(Map(2023 -> config(2023)))

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            profit = 1,
            exemptDistributions = Some(1),
            associatedCompanies = None,
            associatedCompaniesFY1 = None,
            associatedCompaniesFY2 = None
          )
        ) thenReturn Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[PDFView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(
              pdfMetadata = None,
              calculatorResult = calculatorResult,
              accountingPeriodForm = accountingPeriodForm,
              taxableProfit = 1,
              distributions = 1,
              associatedCompanies = Right((1, 2)),
              config = config,
              currentInstant = fakeDateTime.currentInstant,
              request = request,
              messages = messages(application)
            )
            .toString
            .filterAndTrim
        }
      }
    }

    "GET - /pdf-save" - {
      "should download the calculation details PDF when twoAssociatedCompanies is defined" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val requiredAnswersWithAC = requiredAnswers
          .copy()
          .set(
            page = TwoAssociatedCompaniesPage,
            value = TwoAssociatedCompaniesForm(
              associatedCompaniesFY1Count = Some(1),
              associatedCompaniesFY2Count = Some(2)
            )
          )
          .get

        val application = applicationBuilder(userAnswers = Some(requiredAnswersWithAC))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = SingleResult(
          taxDetails = MarginalRate(
            year = accountingPeriodForm.accountingPeriodStartDate.getYear,
            corporationTaxBeforeMR = 1,
            taxRateBeforeMR = 1,
            corporationTax = 1,
            taxRate = 1,
            marginalRelief = 1,
            adjustedProfit = 1,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1,
            adjustedLowerThreshold = 1,
            adjustedUpperThreshold = 1,
            days = 1,
            fyRatio = FYRatio(1, 365)
          ),
          effectiveTaxRate = 1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) thenReturn Future.successful(Map(2023 -> config(2023)))

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            profit = 1,
            exemptDistributions = Some(1),
            associatedCompanies = Some(1),
            associatedCompaniesFY1 = None,
            associatedCompaniesFY2 = None
          )
        ) thenReturn Future.successful(calculatorResult)

        running(application) {
          implicit lazy val materializer: Materializer = application.materializer
          val request = FakeRequest(GET, pdfSaveRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          val contentDisposition = headers(result).get("Content-Disposition")
          contentDisposition.isDefined mustBe true
          contentDisposition.get mustBe s"""attachment; filename="marginal-relief-for-corporation-tax-result-${fakeDateTime.currentInstant
              .atOffset(ZoneOffset.UTC)
              .format(DateTimeFormatter.ofPattern("ddMMyyyy-HHmm"))}.pdf""""
          contentAsBytes(result).nonEmpty mustBe true
        }
      }

      "should download the calculation details PDF when twoAssociatedCompanies is not defined" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(
            year = accountingPeriodForm.accountingPeriodStartDate.getYear,
            corporationTaxBeforeMR = 1,
            taxRateBeforeMR = 1,
            corporationTax = 1,
            taxRate = 1,
            marginalRelief = 1,
            adjustedProfit = 1,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1,
            adjustedLowerThreshold = 1,
            adjustedUpperThreshold = 1,
            days = 1,
            fyRatio = FYRatio(1, 365)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) thenReturn Future.successful(Map(2023 -> config(2023)))

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            profit = 1,
            exemptDistributions = Some(1),
            associatedCompanies = Some(1),
            associatedCompaniesFY1 = None,
            associatedCompaniesFY2 = None
          )
        ) thenReturn Future.successful(calculatorResult)

        running(application) {
          implicit lazy val materializer: Materializer = application.materializer
          val request = FakeRequest(GET, pdfSaveRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          val contentDisposition = headers(result).get("Content-Disposition")
          contentDisposition.isDefined mustBe true
          contentDisposition.get mustBe
            s"""attachment; filename="marginal-relief-for-corporation-tax-result-${fakeDateTime.currentInstant
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("ddMMyyyy-HHmm"))}.pdf""""
          contentAsBytes(result).nonEmpty mustBe true
        }
      }
    }
  }
}
