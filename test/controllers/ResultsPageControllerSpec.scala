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
import connectors.sharedmodel.{ DualResult, FlatRate, MarginalRate, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ AssociatedCompanies, DistributionsIncluded, UserAnswers }
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{ GET, contentAsString, route, running, status, _ }
import uk.gov.hmrc.http.BadRequestException
import views.html.ResultsPageView

import java.time.LocalDate
import scala.concurrent.Future

class ResultsPageControllerSpec extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar {
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private lazy val resultsPageRoute = routes.ResultsPageController.onPageLoad().url

  "ResultsPageController" - {
    "GET page" - {
      "must render results when all required data is available in user answers" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- UserAnswers(userAnswersId)
                    .set(
                      AccountingPeriodPage,
                      AccountingPeriodForm(epoch, Some(epoch.plusDays(1)))
                    )
            u2 <- u1.set(TaxableProfitPage, 1L)
            u3 <- u2.set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None))
            u4 <- u3.set(DistributionsIncludedPage, DistributionsIncludedForm(DistributionsIncluded.Yes, Some(1)))
          } yield u4).toOption
        ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()
        val calculatorResult = SingleResult(MarginalRate(epoch.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = epoch,
          accountingPeriodEnd = epoch.plusDays(1),
          1.0,
          Some(1),
          Some(1)
        )(*) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, resultsPageRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ResultsPageView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(
            calculatorResult,
            AccountingPeriodForm(epoch, Some(epoch.plusDays(1))),
            1,
            1,
            1
          )(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must throw error if any of the required attributes are missing - AccountingPeriodPage" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- UserAnswers(userAnswersId).set(TaxableProfitPage, 1L)
            u2 <- u1.set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None))
            u3 <- u2.set(DistributionsIncludedPage, DistributionsIncludedForm(DistributionsIncluded.Yes, Some(1)))
          } yield u3).toOption
        ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()
        val calculatorResult = SingleResult(MarginalRate(epoch.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = epoch,
          accountingPeriodEnd = epoch.plusDays(1),
          1.0,
          Some(1),
          Some(1)
        )(*) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, resultsPageRoute)
          val result = route(application, request).value.failed.futureValue
          result mustBe a[BadRequestException]
          result.getMessage mustBe "One or more user parameters required for calculation are missing. " +
            "This could be either because the session has expired or the user navigated directly to the results page. " +
            "Missing parameters are [accountingPeriod]"
        }
      }

      "must contain proper wording while accounting period covers 2 financial years" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val startDate = LocalDate.of(2023, 1, 1)
        val endDate = LocalDate.of(2023, 12, 31)

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- UserAnswers(userAnswersId)
                    .set(
                      AccountingPeriodPage,
                      AccountingPeriodForm(startDate, Some(endDate))
                    )
            u2 <- u1.set(TaxableProfitPage, 1L)
            u3 <- u2.set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None))
            u4 <- u3.set(DistributionsIncludedPage, DistributionsIncludedForm(DistributionsIncluded.Yes, Some(1)))
          } yield u4).toOption
        ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()

        val calculatorResult =
          DualResult(MarginalRate(2022, 1, 1, 1, 1, 1, 1, 1, 1, 1, 90), FlatRate(2023, 1, 1, 1, 275))
        mockMarginalReliefCalculatorConnector.calculate(
          accountingPeriodStart = startDate,
          accountingPeriodEnd = endDate,
          1.0,
          Some(1),
          Some(1)
        )(*) returns Future.successful(calculatorResult)

        running(application) {

          val request = FakeRequest(GET, resultsPageRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ResultsPageView]

          val requestContent = contentAsString(result).filterAndTrim
          val viewContent = view(
            calculatorResult,
            AccountingPeriodForm(startDate, Some(endDate)),
            1,
            1,
            1
          )(
            request,
            messages(application)
          ).toString.filterAndTrim

          status(result) mustEqual OK
          requestContent mustEqual viewContent

          requestContent.contains("Covers 2 HMRC financial years") mustEqual true
          requestContent.contains("2022 to 2023: 1 January 2023 to 31 March 2023") mustEqual true
          requestContent.contains("2023 to 2024: 1 April 2023 to 31 December 2023") mustEqual true
        }
      }
    }
  }
}
