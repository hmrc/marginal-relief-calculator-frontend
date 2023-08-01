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
import calculator.{CalculatorValidationResult, MarginalReliefCalculator}
import cats.implicits.catsSyntaxValidatedId
import config.{ConfigMissingError, FrontendAppConfig}
import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel._
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.enablers.Messaging
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, UnprocessableEntityException}

import java.time.LocalDate
import scala.concurrent.Future

class CalculatorServiceSpec extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  trait Test {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    val mockConnector: MarginalReliefCalculatorConnector = mock[MarginalReliefCalculatorConnector]
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val mockCalculator: MarginalReliefCalculator = mock[MarginalReliefCalculator]

    val mockCalculatorService: CalculatorService = new CalculatorService(
      connector = mockConnector, appConfig = mockConfig, calculator = mockCalculator
    )

    val dummyConfig2020: FlatRateConfig = FlatRateConfig(2020, 50)
    val dummyConfig2021: FlatRateConfig = FlatRateConfig(2021, 50)

    def mockReworkEnabledFlag(result: Boolean): ScalaOngoingStubbing[Boolean] = when(
      mockConfig.reworkEnabled
    ).thenReturn(result)

    def mockCalculatorCall(accountingPeriodStart: LocalDate,
                           accountingPeriodEnd: LocalDate,
                           profit: BigDecimal,
                           exemptDistributions: BigDecimal,
                           associatedCompanies: Option[Int],
                           associatedCompaniesFY1: Option[Int],
                           associatedCompaniesFY2: Option[Int],
                           result: CalculatorValidationResult[CalculatorResult]): ScalaOngoingStubbing[CalculatorValidationResult[CalculatorResult]] = when(
      mockCalculator.compute(
        accountingPeriodStart = ArgumentMatchers.eq(accountingPeriodStart),
        accountingPeriodEnd = ArgumentMatchers.eq(accountingPeriodEnd),
        profit = ArgumentMatchers.eq(profit),
        exemptDistributions = ArgumentMatchers.eq(exemptDistributions),
        associatedCompanies = ArgumentMatchers.eq(associatedCompanies),
        associatedCompaniesFY1 = ArgumentMatchers.eq(associatedCompaniesFY1),
        associatedCompaniesFY2 = ArgumentMatchers.eq(associatedCompaniesFY2)
      )
    ).thenReturn(result)

    def mockConnectorCalculateCall(accountingPeriodStart: LocalDate,
                                   accountingPeriodEnd: LocalDate,
                                   profit: Double,
                                   exemptDistributions: Option[Double],
                                   associatedCompanies: Option[Int],
                                   associatedCompaniesFY1: Option[Int],
                                   associatedCompaniesFY2: Option[Int],
                                   result: Future[CalculatorResult]): ScalaOngoingStubbing[Future[CalculatorResult]] =
      when(
        mockConnector.calculate(
          accountingPeriodStart = ArgumentMatchers.eq(accountingPeriodStart),
          accountingPeriodEnd = ArgumentMatchers.eq(accountingPeriodEnd),
          profit = ArgumentMatchers.eq(profit),
          exemptDistributions = ArgumentMatchers.eq(exemptDistributions),
          associatedCompanies = ArgumentMatchers.eq(associatedCompanies),
          associatedCompaniesFY1 = ArgumentMatchers.eq(associatedCompaniesFY1),
          associatedCompaniesFY2 = ArgumentMatchers.eq(associatedCompaniesFY2)
        )(hc = any())
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
      details = FlatRate(
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

    "rework is not enabled should return the expected result" in new Test {
      mockReworkEnabledFlag(result = false)

      mockConnectorCalculateCall(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd,
        profit = profit,
        exemptDistributions = exemptDistributions,
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2,
        result = Future.successful(calculatorResult)
      )

      val result: CalculatorResult = await(mockCalculatorService.calculate(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd,
        profit = profit,
        exemptDistributions = exemptDistributions,
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2
      ))

      result mustBe calculatorResult
    }

    "rework is enabled should return the expected result" in new Test {
      mockReworkEnabledFlag(result = true)

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

      val result: CalculatorResult = await(mockCalculatorService.calculate(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd,
        profit = profit,
        exemptDistributions = exemptDistributions,
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2
      ))

      result mustBe calculatorResult
    }

    "rework is enabled should handle errors" in new Test {
      mockReworkEnabledFlag(result = true)

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


      def result: CalculatorResult = await(mockCalculatorService.calculate(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEndYr2,
        profit = profit,
        exemptDistributions = None,
        associatedCompanies = associatedCompanies,
        associatedCompaniesFY1 = associatedCompaniesFY1,
        associatedCompaniesFY2 = associatedCompaniesFY2
      ))

      implicit val messaging: Messaging[UnprocessableEntityException] = Messaging
        .messagingNatureOfThrowable[UnprocessableEntityException]

      the[UnprocessableEntityException] thrownBy result must have message
        "Failed to calculate marginal relief: " +
          "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 1970, " +
          "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 1971"
    }
  }

}
