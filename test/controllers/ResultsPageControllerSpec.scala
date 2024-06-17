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

import base.SpecBase
import models.calculator.{ DualResult, FYRatio, FlatRate, MarginalRate, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, TwoAssociatedCompaniesForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, UserAnswers }
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import pages._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CalculatorService
import views.html.ResultsPageView

import java.time.LocalDate
import scala.concurrent.Future

class ResultsPageControllerSpec extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar {
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private lazy val resultsPageRoute = routes.ResultsPageController.onPageLoad().url

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(epoch, Some(epoch.plusDays(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.Yes)
    .get
    .set(
      DistributionsIncludedPage,
      DistributionsIncludedForm(
        DistributionsIncluded.Yes,
        Some(1)
      )
    )
    .get
    .set(
      AssociatedCompaniesPage,
      AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))
    )
    .get

  "ResultsPageController" - {
    "GET page" - {
      "must render results when all required data is available in user answers" in {
        val allRequiredAnswers: UserAnswers = requiredAnswers
          .copy()
          .set(
            page = TwoAssociatedCompaniesPage,
            value = TwoAssociatedCompaniesForm(
              associatedCompaniesFY1Count = Some(1),
              associatedCompaniesFY2Count = Some(2)
            )
          )
          .get

        val mockCalculatorService: CalculatorService = mock[CalculatorService]

        val application = applicationBuilder(userAnswers = Some(allRequiredAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .build()

        val calculatorResult = SingleResult(
          MarginalRate(
            year = epoch.getYear,
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
            fyRatio = FYRatio(numerator = 1, denominator = 365)
          ),
          effectiveTaxRate = 1
        )

        mockCalculatorService.calculate(
          accountingPeriodStart = epoch,
          accountingPeriodEnd = epoch.plusDays(1),
          profit = 1.0,
          exemptDistributions = Some(1),
          associatedCompanies = Some(1),
          associatedCompaniesFY1 = Some(1),
          associatedCompaniesFY2 = Some(2)
        ) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, resultsPageRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ResultsPageView]

          status(result) mustEqual OK

          contentAsString(result).filterAndTrim mustEqual view
            .render(
              calculatorResult = calculatorResult,
              accountingPeriodForm = AccountingPeriodForm(
                accountingPeriodStartDate = epoch,
                accountingPeriodEndDate = Some(epoch.plusDays(1))
              ),
              taxableProfit = 1,
              distributions = 1,
              associatedCompanies = Right((1, 2)),
              request = request,
              messages = messages(application)
            )
            .toString
            .filterAndTrim
        }
      }

      "must redirect to Journey recovery if required params are missing in user answers" in {
        val mockCalculatorService: CalculatorService =
          mock[CalculatorService]

        val application = applicationBuilder(
          userAnswers = Some(
            emptyUserAnswers
              .set(AccountingPeriodPage, AccountingPeriodForm(epoch, Some(epoch.plusDays(1))))
              .get
          )
        ).overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .build()
        val calculatorResult =
          SingleResult(MarginalRate(epoch.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 365)), 1)
        mockCalculatorService.calculate(
          accountingPeriodStart = epoch,
          accountingPeriodEnd = epoch.plusDays(1),
          1.0,
          Some(1),
          Some(1),
          None,
          None
        ) returns Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, resultsPageRoute)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must contain proper wording while accounting period covers 2 financial years" in {
        val mockCalculatorService: CalculatorService =
          mock[CalculatorService]

        val startDate = LocalDate.of(2023, 1, 1)
        val endDate = LocalDate.of(2023, 12, 31)

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- emptyUserAnswers
                    .set(
                      AccountingPeriodPage,
                      AccountingPeriodForm(startDate, Some(endDate))
                    )
            u2 <- u1.set(TaxableProfitPage, 1)
            u3 <- u2.set(DistributionPage, Distribution.Yes)
            u4 <- u3.set(DistributionsIncludedPage, DistributionsIncludedForm(DistributionsIncluded.Yes, Some(1)))
            u5 <- u4.set(
                    AssociatedCompaniesPage,
                    AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))
                  )
          } yield u5).toOption
        ).overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .build()

        val calculatorResult =
          DualResult(
            MarginalRate(2022, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 90, FYRatio(90, 365)),
            FlatRate(2023, 1, 1, 1, 1, 1, 275),
            1
          )
        mockCalculatorService.calculate(
          accountingPeriodStart = startDate,
          accountingPeriodEnd = endDate,
          1.0,
          Some(1),
          Some(1),
          None,
          None
        ) returns Future.successful(calculatorResult)

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
            Left(1)
          )(
            request,
            messages(application)
          ).toString.filterAndTrim

          status(result) mustEqual OK
          requestContent mustEqual viewContent
          requestContent.contains("2022 to 2023: 1 January 2023 to 31 March 2023") mustEqual true
          requestContent.contains("2023 to 2024: 1 April 2023 to 31 December 2023") mustEqual true
        }
      }
    }
  }
}
