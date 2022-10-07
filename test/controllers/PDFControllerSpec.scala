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

import base.SpecBase
import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel.{ DualResult, MarginalRate, MarginalReliefConfig, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded }
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.PDFView

import java.time.LocalDate
import scala.concurrent.Future

class PDFControllerSpec extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar {

  private lazy val pdfViewRoute = routes.PDFController.onPageLoad().url

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
      AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))
    )
    .get

  "PDFController" - {
    "GET page" - {
      "must render pdf page when all data is available for single year" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(accountingPeriodForm.accountingPeriodStartDate.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
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
            .render(calculatorResult, accountingPeriodForm, 1, 1, 1, config, request, messages(application))
            .toString
            .filterAndTrim
        }
      }
      "must render pdf page when all data is available for dual year" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()

        val calculatorResult = DualResult(
          MarginalRate(accountingPeriodForm.accountingPeriodStartDate.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
          MarginalRate(accountingPeriodForm.accountingPeriodStartDate.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
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
            .render(calculatorResult, accountingPeriodForm, 1, 1, 1, config, request, messages(application))
            .toString
            .filterAndTrim
        }
      }
    }
  }
}
