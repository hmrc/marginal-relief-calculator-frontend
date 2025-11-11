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
import models.calculator.*
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, TwoAssociatedCompaniesForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, MarginalReliefConfig }
import pages.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{ CalculationConfigService, CalculatorService }
import views.html.FullResultsPageView
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate
import scala.concurrent.Future

class FullResultsPageControllerSpec extends SpecBase with MockitoSugar {

  private lazy val fullResultsPageRoute = routes.FullResultsPageController.onPageLoad().url

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
      AssociatedCompaniesForm(AssociatedCompanies.Yes, None)
    )
    .get
    .set(
      TwoAssociatedCompaniesPage,
      TwoAssociatedCompaniesForm(Some(1), Some(2))
    )
    .get

  private val oneAssociatedCompany = Left(1)
  private val twoAssociatedCompanies = Right((1, 2))

  "FullResultsPageController" - {
    "GET page" - {
      "must render results when all data is available" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
          .build()

        val calculatorResult = DualResult(
          year1TaxDetails = MarginalRate(
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
            FYRatio(1, 1)
          ),
          year2TaxDetails = MarginalRate(
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
            FYRatio(1, 1)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) `thenReturn` Future.successful(
          Map(2023 -> config(2023))
        )

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            1,
            Some(1),
            None,
            Some(1),
            Some(2)
          )
        ) `thenReturn` Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, fullResultsPageRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FullResultsPageView]

          status(result) `mustEqual` OK
          contentAsString(result).filterAndTrim `mustEqual` view
            .render(
              calculatorResult,
              accountingPeriodForm,
              1,
              1,
              twoAssociatedCompanies,
              config,
              request,
              messages(application)
            )
            .toString
            .filterAndTrim
        }
      }

      "must render results when distributions is No" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .set(AccountingPeriodPage, accountingPeriodForm)
              .get
              .set(TaxableProfitPage, 1)
              .get
              .set(DistributionPage, Distribution.No)
              .get
              .set(
                AssociatedCompaniesPage,
                associatedCompaniesForm
              )
              .get
          )
        )
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
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
            FYRatio(1, 1)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) `thenReturn` Future.successful(
          Map(2023 -> config(2023))
        )

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            1,
            None,
            Some(1),
            None,
            None
          )
        ) `thenReturn` Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, fullResultsPageRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FullResultsPageView]

          status(result) `mustEqual` OK
          contentAsString(result).filterAndTrim `mustEqual` view(
            calculatorResult,
            accountingPeriodForm,
            1,
            0,
            oneAssociatedCompany,
            config
          )(request, messages(application)).toString.filterAndTrim
        }
      }

      "must render results when distributions and associated companies is No" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .set(AccountingPeriodPage, accountingPeriodForm)
              .get
              .set(TaxableProfitPage, 1)
              .get
              .set(DistributionPage, Distribution.No)
              .get
              .set(
                AssociatedCompaniesPage,
                AssociatedCompaniesForm(AssociatedCompanies.No, None)
              )
              .get
          )
        )
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
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
            FYRatio(1, 1)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) `thenReturn` Future.successful(
          Map(2023 -> config(2023))
        )

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            1,
            None,
            None,
            None,
            None
          )
        ) `thenReturn` Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, fullResultsPageRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[FullResultsPageView]

          status(result) `mustEqual` OK
          contentAsString(result).filterAndTrim `mustEqual` view(
            calculatorResult,
            accountingPeriodForm,
            1,
            0,
            Left(0),
            config
          )(request, messages(application)).toString.filterAndTrim
        }
      }

      "must redirect to Journey recovery when mandatory parameters are missing in user answers" in {
        val mockCalculatorService: CalculatorService = mock[CalculatorService]
        val mockConfigService: CalculationConfigService = mock[CalculationConfigService]

        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .set(AccountingPeriodPage, accountingPeriodForm)
              .get
          )
        )
          .overrides(bind[CalculatorService].toInstance(mockCalculatorService))
          .overrides(bind[CalculationConfigService].toInstance(mockConfigService))
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
            FYRatio(1, 1)
          ),
          1
        )

        when(mockConfigService.getAllConfigs(calculatorResult)) `thenReturn` Future.successful(
          Map(2023 -> config(2023))
        )

        when(
          mockCalculatorService.calculate(
            accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault,
            1,
            None,
            None,
            None,
            None
          )
        ) `thenReturn` Future.successful(calculatorResult)

        running(application) {
          val request = FakeRequest(GET, fullResultsPageRoute)
          val result = route(application, request).value
          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value `mustEqual` routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
