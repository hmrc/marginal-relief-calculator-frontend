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
import config.FrontendAppConfig
import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel._
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ ArgumentMatchers, MockitoSugar }
import org.scalatest.enablers.Messaging
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import uk.gov.hmrc.http.{ HeaderCarrier, UnprocessableEntityException }
import utils.ShowCalculatorDisclaimerUtils.financialYearEnd

import java.time.LocalDate
import scala.concurrent.Future

class AssociatedCompaniesParameterServiceSpec
    extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  trait Test {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    val mockConnector: MarginalReliefCalculatorConnector = mock[MarginalReliefCalculatorConnector]
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

    val mockParameterService: AssociatedCompaniesParameterService = new AssociatedCompaniesParameterService(
      connector = mockConnector,
      appConfig = mockConfig
    )

    val dummyConfig2020: FlatRateConfig = FlatRateConfig(2020, 50)
    val dummyConfig2021: FlatRateConfig = FlatRateConfig(2021, 50)

    def mockReworkEnabledFlag(result: Boolean): ScalaOngoingStubbing[Boolean] = when(
      mockConfig.reworkEnabled
    ).thenReturn(result)

    def mockCalculatorConfig(result: CalculatorConfig): ScalaOngoingStubbing[CalculatorConfig] = when(
      mockConfig.calculatorConfig
    ).thenReturn(result)

    def mockConnectorCall(
      accountingPeriodStart: LocalDate,
      accountingPeriodEnd: LocalDate,
      result: Future[AssociatedCompaniesParameter]
    ): ScalaOngoingStubbing[Future[AssociatedCompaniesParameter]] = when(
      mockConnector.associatedCompaniesParameters(
        accountingPeriodStart = ArgumentMatchers.eq(accountingPeriodStart),
        accountingPeriodEnd = ArgumentMatchers.eq(accountingPeriodEnd)
      )(
        hc = any()
      )
    ).thenReturn(result)
  }

  "associatedCompaniesParameters" - {
    val accountingPeriodStart: LocalDate = LocalDate.of(2023, 1, 1)
    val accountingPeriodEnd: LocalDate = LocalDate.of(2023, 1, 2)

    "rework is not enabled should return expected result" in new Test {
      mockReworkEnabledFlag(result = false)

      mockConnectorCall(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd,
        result = Future.successful(AskFull)
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = accountingPeriodEnd
        )
      )

      result mustBe AskFull
    }

    "rework is enabled should return DontAsk for single flat rate tax year" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            FlatRateConfig(year = 2022, mainRate = 50.0)
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = accountingPeriodEnd
        )
      )

      result mustBe DontAsk
    }

    "rework is enabled should return AskFull for single marginal rate tax year" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            MarginalReliefConfig(
              year = 2022,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            )
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = accountingPeriodEnd
        )
      )

      result mustBe AskFull
    }

    val secondYearAccountingPeriodEnd: LocalDate = accountingPeriodEnd.plusYears(1)
    val fyEndForAccountingPeriodStart: LocalDate = financialYearEnd(accountingPeriodStart)

    "rework is enabled should return DontAsk for two flat rate tax years" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            FlatRateConfig(year = 2022, mainRate = 50.0),
            FlatRateConfig(year = 2023, mainRate = 50.0)
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      result mustBe DontAsk
    }

    "rework is enabled should return AskFull for two marginal rate tax years with same thresholds" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            MarginalReliefConfig(
              year = 2022,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            ),
            MarginalReliefConfig(
              year = 2023,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            )
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      result mustBe AskFull
    }

    val taxYear2Start: LocalDate = fyEndForAccountingPeriodStart.plusDays(1)

    "rework is enabled should return AskBothParts for two marginal rate tax years with different thresholds" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            MarginalReliefConfig(
              year = 2022,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            ),
            MarginalReliefConfig(
              year = 2023,
              lowerThreshold = 0,
              upperThreshold = 12,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            )
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      result mustBe AskBothParts(
        period1 = Period(start = accountingPeriodStart, end = fyEndForAccountingPeriodStart),
        period2 = Period(start = taxYear2Start, end = secondYearAccountingPeriodEnd)
      )
    }

    "rework is enabled should return AskOnePart when first year only is marginal" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            MarginalReliefConfig(
              year = 2022,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            ),
            FlatRateConfig(year = 2023, mainRate = 50.0)
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      result mustBe AskOnePart(period =
        Period(
          start = accountingPeriodStart,
          end = fyEndForAccountingPeriodStart
        )
      )
    }

    "rework is enabled should return AskOnePart when second year only is marginal" in new Test {
      mockReworkEnabledFlag(result = true)

      mockCalculatorConfig(result =
        CalculatorConfig(fyConfigs =
          Seq(
            FlatRateConfig(year = 2022, mainRate = 50.0),
            MarginalReliefConfig(
              year = 2023,
              lowerThreshold = 0,
              upperThreshold = 10,
              smallProfitRate = 0.5,
              mainRate = 0.7,
              marginalReliefFraction = 0.5
            )
          )
        )
      )

      val result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      result mustBe AskOnePart(period = Period(start = taxYear2Start, secondYearAccountingPeriodEnd))
    }

    "rework is enabled should handle errors" in new Test {
      mockReworkEnabledFlag(result = true)
      mockCalculatorConfig(result = CalculatorConfig(fyConfigs = Seq()))

      def result: AssociatedCompaniesParameter = await(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = accountingPeriodStart,
          accountingPeriodEnd = secondYearAccountingPeriodEnd
        )
      )

      implicit val messaging: Messaging[UnprocessableEntityException] = Messaging
        .messagingNatureOfThrowable[UnprocessableEntityException]

      the[UnprocessableEntityException] thrownBy result must have message
        "Failed to determined associated company parameters for given data: " +
        "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 2022, " +
        "uk.gov.hmrc.http.UnprocessableEntityException: Configuration missing for financial year: 2023"
    }
  }
}
