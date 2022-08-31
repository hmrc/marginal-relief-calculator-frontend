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
import connectors.sharedmodel.{ DualResult, FlatRate, FlatRateConfig, MarginalRate, MarginalReliefConfig, SingleResult }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, ResultsPageData, UserAnswers }
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{ GET, contentAsString, route, running, status, _ }
import uk.gov.hmrc.http.BadRequestException
import views.html.{ FullResultsPageView, ResultsPageView }

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Try

class ResultsPageControllerSpec extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar {
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private lazy val resultsPageRoute = routes.ResultsPageController.onPageLoad().url
  private lazy val fullResultsPageRoute = routes.ResultsPageController.fullResultsOnPageLoad().url

  private val userAnswersF = (for {
    u <- UserAnswers(
           "test-session-id"
         ).set(
           AccountingPeriodPage,
           AccountingPeriodForm(
             LocalDate.parse("2022-03-23"),
             Some(LocalDate.parse("2022-04-23"))
           )
         )
    u1 <- u.set(TaxableProfitPage, 70000)
    u2 <- u1.set(
            DistributionPage,
            Distribution.Yes
          )
    u3 <- u2.set(
            DistributionsIncludedPage,
            DistributionsIncludedForm(
              DistributionsIncluded.Yes,
              Some(6000)
            )
          )
    u4 <- u3.set(
            AssociatedCompaniesPage,
            AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None)
          )
  } yield u4).get

  private val calcResultF = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2024-01-01"), Some(LocalDate.parse("2024-02-01"))),
    70000,
    SingleResult(FlatRate(2022, 3279.45, 19.0, 17260.27, 90)),
    6000,
    1
  )

  private val userAnswersM = userAnswersF
    .set(
      AccountingPeriodPage,
      AccountingPeriodForm(
        LocalDate.parse("2024-01-01"),
        Some(LocalDate.parse("2024-02-01"))
      )
    )
    .get

  private val calcResultM = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2024-01-01"), Some(LocalDate.parse("2024-02-01"))),
    70000,
    SingleResult(MarginalRate(2023, 17500.0, 25.0, 17500.0, 25.0, 0.0, 70000.0, 6000.0, 2191.78, 10958.9, 32)),
    6000,
    1
  )

  private val userAnswersFF = userAnswersF
    .set(
      AccountingPeriodPage,
      AccountingPeriodForm(
        LocalDate.parse("2021-01-01"),
        Some(LocalDate.parse("2021-12-31"))
      )
    )
    .get

  private val calcResultFF = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2023-01-01"), Some(LocalDate.parse("2023-12-31"))),
    70000,
    DualResult(
      FlatRate(2022, 3279.45, 19.0, 17260.27, 90),
      FlatRate(2023, 3279.45, 19.0, 17260.27, 90)
    ),
    6000,
    1
  )

  private val userAnswersMM = userAnswersF
    .set(
      AccountingPeriodPage,
      AccountingPeriodForm(
        LocalDate.parse("2024-01-01"),
        Some(LocalDate.parse("2024-12-31"))
      )
    )
    .get
    .set(
      AssociatedCompaniesPage,
      AssociatedCompaniesForm(AssociatedCompanies.No, None, None, None)
    )
    .get
  private val calcResultMM = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2024-01-01"), Some(LocalDate.parse("2024-12-31"))),
    70000,
    DualResult(
      MarginalRate(2023, 4351.09, 25.0, 3753.39, 21.57, 597.7, 17404.37, 1491.8, 12431.69, 62158.47, 91),
      MarginalRate(2024, 13148.91, 25.0, 11281.86, 21.45, 1867.05, 52595.63, 4508.2, 37671.23, 226027.4, 275)
    ),
    6000,
    0
  )
  private val userAnswersMF = userAnswersF
    .set(
      AccountingPeriodPage,
      AccountingPeriodForm(
        LocalDate.parse("2025-01-01"),
        Some(LocalDate.parse("2025-12-31"))
      )
    )
    .get

  private val calcResultMF = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2025-01-01"), Some(LocalDate.parse("2025-12-31"))),
    70000,
    DualResult(
      MarginalRate(2024, 4315.07, 25.0, 4113.4, 23.83, 201.67, 17260.27, 1479.45, 6164.38, 36986.3, 90),
      FlatRate(2025, 10020.55, 19.0, 52739.73, 275)
    ),
    6000,
    1
  )
  private val userAnswersFM = userAnswersF
    .set(
      AccountingPeriodPage,
      AccountingPeriodForm(
        LocalDate.parse("2023-01-01"),
        Some(LocalDate.parse("2023-12-31"))
      )
    )
    .get
  private val calcResultFM = ResultsPageData(
    AccountingPeriodForm(LocalDate.parse("2023-01-01"), Some(LocalDate.parse("2023-12-31"))),
    70000,
    DualResult(
      FlatRate(2022, 3279.45, 19.0, 17260.27, 90),
      MarginalRate(2023, 13184.93, 25.0, 12674.88, 24.03, 510.05, 52739.73, 4520.55, 18835.62, 94178.08, 275)
    ),
    6000,
    1
  )

  private val config = Map(
    2022 -> FlatRateConfig(2022, 0.19),
    2023 -> MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015),
    2024 -> MarginalReliefConfig(2024, 50000, 300000, 0.19, 0.25, 0.012),
    2025 -> FlatRateConfig(2025, 0.19)
  )

  private def fullResultPageSetup(userAnswers: UserAnswers, resultsPageData: ResultsPageData)(
    f: (Application, Future[Result], String) => Unit
  ): Unit = {
    val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
      mock[MarginalReliefCalculatorConnector]
    val application = applicationBuilder(Some(userAnswers))
      .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
      .build()

    val ResultsPageData(
      accountingPeriodForm,
      taxableProfit,
      calculatorResult,
      distributionsIncludedAmount,
      associatedCompaniesCount
    ) = resultsPageData

    val view = application.injector.instanceOf[FullResultsPageView]

    val request = FakeRequest(GET, fullResultsPageRoute)

    val result = route(application, request).value

    // TODO: Mock properly - Pavel Vjalicin
    mockMarginalReliefCalculatorConnector.calculate(
      *, // accountingPeriodStart = userAnswers.get(AccountingPeriodPage).get.accountingPeriodStartDate,
      *, // accountingPeriodEnd = userAnswers.get(AccountingPeriodPage).get.accountingPeriodEndDate.get,
      *, // userAnswers.get(TaxableProfitPage).get,
      *, // userAnswers.get(DistributionsIncludedPage).get.distributionsIncludedAmount.map(_.toDouble),
      *, // userAnswers.get(AssociatedCompaniesPage).get.associatedCompaniesCount,
      *, // None,
      * // None
    )(*) returns Future.successful(calculatorResult)

    val viewContent = view(
      calculatorResult,
      accountingPeriodForm,
      taxableProfit,
      distributionsIncludedAmount,
      associatedCompaniesCount,
      config
    )(request, messages(application)).toString.filterAndTrim

    running(application) {
      f(application, result, viewContent)
    }
  }

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
            u2 <- u1.set(TaxableProfitPage, 1)
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
          Some(1),
          None,
          None
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
            u1 <- UserAnswers(userAnswersId).set(TaxableProfitPage, 1)
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
          Some(1),
          None,
          None
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
            u2 <- u1.set(TaxableProfitPage, 1)
            u3 <- u2.set(
                    AssociatedCompaniesPage,
                    AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), Some(2), Some(3))
                  )
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
          Some(1),
          Some(2),
          Some(3)
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
          requestContent.contains("2022 to 2023: 1 January 2023 to 31 March 2023") mustEqual true
          requestContent.contains("2023 to 2024: 1 April 2023 to 31 December 2023") mustEqual true
        }
      }
    }
    "Full results page" - {

      "properly renders MF results" in {}

      "shows tabs when 2 marginal years are selected" in {
        fullResultPageSetup(userAnswersMM, calcResultMM) { (app, result, view) =>
          val requestContent = contentAsString(result).filterAndTrim

          status(result) mustEqual OK

          requestContent mustEqual view

          requestContent.contains("""<div class="govuk-tabs" data-module="govuk-tabs">""") mustEqual true
        }
      }
      "does not show tabs when only 1 marginal year is selected" in {

        val oneMarginYearScenarios = Seq(
          (userAnswersMF, calcResultMF),
          (userAnswersFM, calcResultFM),
          (userAnswersM, calcResultM)
        )

        oneMarginYearScenarios.foreach { case (userAnswers, calcResult) =>
          fullResultPageSetup(userAnswers, calcResult) { (app, result, view) =>
            val requestContent = contentAsString(result).filterAndTrim

            status(result) mustEqual OK

            requestContent mustEqual view

            requestContent.contains("""<div class="govuk-tabs" data-module="govuk-tabs">""") mustEqual false
          }
        }

      }

      "throw error when no marginal year is available" in {
        val noMarginalYears = Seq(
          (userAnswersFF, calcResultFF),
          (userAnswersF, calcResultF)
        )

        noMarginalYears.foreach { case (userAnswers, calcResult) =>
          Try(fullResultPageSetup(userAnswers, calcResult)((app, result, view) => ())).failed.get mustBe a[
            RuntimeException
          ]
        }
      }
    }
  }
}
