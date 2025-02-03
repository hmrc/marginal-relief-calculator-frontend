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
import config.{ CalculatorConfig, FrontendAppConfig }
import models.calculator.{ CalculatorResult, DualResult, FlatRate, SingleResult }
import models.{ FYConfig, FlatRateConfig }
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class CalculationConfigServiceSpec extends SpecBase with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  trait Test {
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

    val mockCalculationConfigService: CalculationConfigService = new CalculationConfigService(
      appConfig = mockConfig
    )

    val dummyConfig2020: FlatRateConfig = FlatRateConfig(2020, 50)
    val dummyConfig2021: FlatRateConfig = FlatRateConfig(2021, 50)

    def mockCalculatorConfig(result: CalculatorConfig): OngoingStubbing[CalculatorConfig] = when(
      mockConfig.calculatorConfig
    ).thenReturn(result)

  }

  "getConfig" - {

    "rework is enabled should return expected config from configuration file" in new Test {
      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020)))

      val result: FYConfig = await(mockCalculationConfigService.getConfig(2020))
      result mustBe dummyConfig2020
    }

    "rework is enabled should handle config error" in new Test {
      mockCalculatorConfig(CalculatorConfig(Seq.empty[FYConfig]))

      def result: FYConfig = await(mockCalculationConfigService.getConfig(2020))
      the[RuntimeException] thrownBy result must have message "Configuration for year 2020 is missing."

    }
  }

  "getAllConfigs" - {
    "return single config result for request contained within a single tax year" in new Test {
      val dummyCalculator2020Result: CalculatorResult = SingleResult(
        taxDetails = FlatRate(
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

      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020)))

      val result: Map[Int, FYConfig] = await(
        mockCalculationConfigService.getAllConfigs(dummyCalculator2020Result)
      )

      result mustBe Map(2020 -> dummyConfig2020)
    }

    "return two config results for request which covers two tax years" in new Test {
      val dummyCalculatorDualResult: CalculatorResult = DualResult(
        year1TaxDetails = FlatRate(
          year = 2020,
          corporationTax = 1.0,
          taxRate = 11.0,
          adjustedProfit = 111.0,
          adjustedDistributions = 0,
          adjustedAugmentedProfit = 1,
          days = 1
        ),
        year2TaxDetails = FlatRate(
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

      mockCalculatorConfig(CalculatorConfig(Seq(dummyConfig2020, dummyConfig2021)))

      val result: Map[Int, FYConfig] = await(
        mockCalculationConfigService.getAllConfigs(dummyCalculatorDualResult)
      )

      result mustBe Map(2020 -> dummyConfig2020, 2021 -> dummyConfig2021)

    }
  }

}
