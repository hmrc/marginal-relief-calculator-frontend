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

import akka.stream.Materializer
import base.SpecBase
import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel.{ DualResult, FYRatio, MarginalRate, MarginalReliefConfig, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, PDFAddCompanyDetailsForm, PDFMetadataForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, PDFAddCompanyDetails }
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, PDFAddCompanyDetailsPage, PDFMetadataPage, TaxableProfitPage }
import play.api.http.Status.OK
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{ GET, contentAsBytes, contentAsString, defaultAwaitTimeout, headers, route, running, status, writeableOf_AnyContentAsEmpty }
import utils.{ DateTime, FakeDateTime }
import views.html.PDFView

import java.time.{ LocalDate, ZoneOffset }
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class PDFControllerSpec extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar {

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
      "must render pdf page when all data is available for single year" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(
            accountingPeriodForm.accountingPeriodStartDate.getYear,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            FYRatio(1, 365)
          ),
          1
        )

        mockMarginalReliefCalculatorConnector.config(2023)(*) returns Future.successful(config(2023))

        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
          accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
          1,
          Some(1),
          Some(1),
          None,
          None
        )(*) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[PDFView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              1,
              1,
              Left(1),
              config,
              fakeDateTime.currentInstant,
              request,
              messages(application)
            )
            .toString
            .filterAndTrim
        }
      }

      "must render redirect to recovery controller when all data is not available" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          header(HeaderNames.LOCATION, result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
        }
      }

      "must render pdf page when all data is available for dual year" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = DualResult(
          MarginalRate(
            accountingPeriodForm.accountingPeriodStartDate.getYear,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            FYRatio(1, 365)
          ),
          MarginalRate(
            accountingPeriodForm.accountingPeriodStartDate.getYear,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            FYRatio(1, 365)
          ),
          1
        )

        mockMarginalReliefCalculatorConnector.config(2023)(*) returns Future.successful(config(2023))

        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
          accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
          1,
          Some(1),
          Some(1),
          None,
          None
        )(*) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, pdfViewRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[PDFView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              1,
              1,
              Left(1),
              config,
              fakeDateTime.currentInstant,
              request,
              messages(application)
            )
            .toString
            .filterAndTrim
        }
      }
    }

    "GET - /pdf-save" - {
      "should download the calculation details PDF" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .overrides(bind[DateTime].toInstance(fakeDateTime))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(
            accountingPeriodForm.accountingPeriodStartDate.getYear,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            FYRatio(1, 365)
          ),
          1
        )

        mockMarginalReliefCalculatorConnector.config(2023)(*) returns Future.successful(config(2023))

        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
          accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
          1,
          Some(1),
          Some(1),
          None,
          None
        )(*) returns Future.successful(calculatorResult)

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
    }
  }
}
