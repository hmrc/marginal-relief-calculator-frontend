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
import connectors.sharedmodel.{CalculatorConfig, CalculatorResult, DualResult, FYConfig, FlatRate, FlatRateConfig, SingleResult}
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentMatchers, MockitoSugar}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CalculationConfigServiceSpec extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  trait Test {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    val mockConnector: MarginalReliefCalculatorConnector = mock[MarginalReliefCalculatorConnector]
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

    val mockCalculationConfigService: CalculationConfigService = new CalculationConfigService(
      connector = mockConnector, appConfig = mockConfig
    )

    val dummyConfig2020: FlatRateConfig = FlatRateConfig(2020, 50)
    val dummyConfig2021: FlatRateConfig = FlatRateConfig(2021, 50)

    def mockReworkEnabledFlag(result: Boolean): ScalaOngoingStubbing[Boolean] = when(
      mockConfig.reworkEnabled
    ).thenReturn(result)

    def mockCalculatorConfig(result: CalculatorConfig): ScalaOngoingStubbing[CalculatorConfig] = when(
      mockConfig.calculatorConfig
    ).thenReturn(result)

    def mockConnectorConfigCall(year: Int, result: Future[FYConfig]): ScalaOngoingStubbing[Future[FYConfig]] = when(
      mockConnector.config(year = ArgumentMatchers.eq(year))(hc = any())
    ).thenReturn(result)
  }

  "getConfig" - {
    "rework is not enabled should return expected config from connector" in new Test {
      mockReworkEnabledFlag(result = false)
      mockConnectorConfigCall(year = 2020, result = Future.successful(dummyConfig2020))

      val result: FYConfig = await(mockCalculationConfigService.getConfig(2020))
      result mustBe dummyConfig2020
    }

    "rework is enabled should return expected config from configuration file" in new Test {
      mockReworkEnabledFlag(result = true)
      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020)))

      val result: FYConfig = await(mockCalculationConfigService.getConfig(2020))
      result mustBe dummyConfig2020
    }

    "rework is enabled should handle config error" in new Test {
      mockReworkEnabledFlag(result = true)
      mockCalculatorConfig(CalculatorConfig(Seq.empty[FYConfig]))

      def result: FYConfig = await(mockCalculationConfigService.getConfig(2020))
      the [RuntimeException] thrownBy result must have message "Configuration for year 2020 is missing."

    }
  }

  "getAllConfigs" - {
    "return single config result for request contained within a single tax year" in new Test {
      val dummyCalculator2020Result: CalculatorResult = SingleResult(
        details = FlatRate(
          year = 2020,
          corporationTax = 1.0,
          taxRate = 11.0,
          adjustedProfit = 111.0,
          adjustedDistributions = 0,
          adjustedAugmentedProfit = 1,
          days = 1
        ),
        effectiveTaxRate = 50
      )

      mockReworkEnabledFlag(result = true)
      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020)))

      val result: Map[Int, FYConfig] = await(
        mockCalculationConfigService.getAllConfigs(dummyCalculator2020Result)
      )

      result mustBe Map(2020 -> dummyConfig2020)
    }

    "return two config results for request which covers two tax years" in new Test {
      val dummyCalculatorDualResult: CalculatorResult = DualResult(
        year1 = FlatRate(
          year = 2020,
          corporationTax = 1.0,
          taxRate = 11.0,
          adjustedProfit = 111.0,
          adjustedDistributions = 0,
          adjustedAugmentedProfit = 1,
          days = 1
        ),
        year2 = FlatRate(
          year = 2021,
          corporationTax = 1.0,
          taxRate = 11.0,
          adjustedProfit = 111.0,
          adjustedDistributions = 0,
          adjustedAugmentedProfit = 1,
          days = 1
        ),
        effectiveTaxRate = 50
      )

      mockReworkEnabledFlag(result = true)
      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020, dummyConfig2021)))

      val result: Map[Int, FYConfig] = await(
        mockCalculationConfigService.getAllConfigs(dummyCalculatorDualResult)
      )

      result mustBe Map(2020 -> dummyConfig2020, 2021 -> dummyConfig2021)

    }
  }

}
