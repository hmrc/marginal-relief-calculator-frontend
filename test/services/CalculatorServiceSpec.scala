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

package services

import base.SpecBase
import cats.implicits.catsSyntaxValidatedId
import config.{ ConfigMissingError, FrontendAppConfig }
import models.FlatRateConfig
import models.calculator.*
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import uk.gov.hmrc.http.{ HeaderCarrier, UnprocessableEntityException }

import java.time.LocalDate

class CalculatorServiceSpec extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  trait Test {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val mockCalculator: MarginalReliefCalculatorService = mock[MarginalReliefCalculatorService]

    val mockCalculatorService: CalculatorService = new CalculatorService()(
      using appConfig = mockConfig,
      calculator = mockCalculator
    )

    val dummyConfig2020: FlatRateConfig = FlatRateConfig(2020, 50)
    val dummyConfig2021: FlatRateConfig = FlatRateConfig(2021, 50)

    def mockCalculatorCall(
      accountingPeriodStart: LocalDate,
      accountingPeriodEnd: LocalDate,
      profit: BigDecimal,
      exemptDistributions: BigDecimal,
      associatedCompanies: Option[Int],
      associatedCompaniesFY1: Option[Int],
      associatedCompaniesFY2: Option[Int],
      result: mockCalculator.ValidationResult[CalculatorResult]
    ): OngoingStubbing[mockCalculator.ValidationResult[CalculatorResult]] = when(
      mockCalculator.compute(
        accountingPeriodStart = ArgumentMatchers.eq(accountingPeriodStart),
        accountingPeriodEnd = ArgumentMatchers.eq(accountingPeriodEnd),
        profit = ArgumentMatchers.eq(profit),
        distributions = ArgumentMatchers.eq(exemptDistributions),
        associatedCompanies = ArgumentMatchers.eq(associatedCompanies),
        associatedCompaniesFY1 = ArgumentMatchers.eq(associatedCompaniesFY1),
        associatedCompaniesFY2 = ArgumentMatchers.eq(associatedCompaniesFY2)
      )
    ).thenReturn(result)
  }

  "calculate" - {
    val accountingPeriodStart: LocalDate = LocalDate.ofEpochDay(0)
    val accountingPeriodEnd: LocalDate = LocalDate.ofEpochDay(0)
    val profit: Double = 1
    val exemptDistributions: Option[Double] = Some(1)
    val associatedCompanies: Option[Int] = Some(1)
    val associatedCompaniesFY1: Option[Int] = Some(2)
    val associatedCompaniesFY2: Option[Int] = Some(3)

    val calculatorResult: CalculatorResult = SingleResult(
      taxDetails = FlatRate(
        year = 2023,
        corporationTax = 1.0,
        taxRate = 11.0,
        adjustedProfit = 111.0,
        adjustedDistributions = 0,
        adjustedAugmentedProfit = 1,
        days = 1
      ),
      effectiveTaxRate = 0.5
    )

    "rework is enabled should return the expected result" in new Test {
      mockCalculatorCall(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd,
        profit = profit,
        exemptDistributions = BigDecimal(exemptDistributions.get),
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2,
        result = calculatorResult.validNel
      )

      val result: CalculatorResult = await(
        mockCalculatorService.calculate(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = accountingPeriodEnd,
          profit = profit,
          exemptDistributions = exemptDistributions,
          associatedCompanies = associatedCompanies,
          associatedCompaniesFY1 = associatedCompaniesFY1,
          associatedCompaniesFY2 = associatedCompaniesFY2
        )
      )

      result mustBe calculatorResult
    }

    "rework is enabled should handle errors" in new Test {
      val accountingPeriodEndYr2: LocalDate = accountingPeriodEnd.plusYears(1)

      mockCalculatorCall(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEndYr2,
        profit = profit,
        exemptDistributions = BigDecimal(0.0),
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2,
        result = ConfigMissingError(year = 1970).invalidNel.leftMap(errs => errs :+ ConfigMissingError(year = 1971))
      )

      def result: CalculatorResult = await(
        mockCalculatorService.calculate(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = accountingPeriodEndYr2,
          profit = profit,
          exemptDistributions = None,
          associatedCompanies = associatedCompanies,
          associatedCompaniesFY1 = associatedCompaniesFY1,
          associatedCompaniesFY2 = associatedCompaniesFY2
        )
      )

      val exception: UnprocessableEntityException = intercept[UnprocessableEntityException](result)

      exception.getMessage shouldBe {
        "Failed to calculate marginal relief: " +
          "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 1970, " +
          "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 1971"
      }
    }
  }
}
