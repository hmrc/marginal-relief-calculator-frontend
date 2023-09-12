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

package calculator

import models.{ FlatRateConfig, MarginalReliefConfig }
import models.calculator.{ DualResult, FYRatio, FlatRate, FyDataWrapper, MarginalRate }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class MixedRateCalculatorSpec extends AnyWordSpec with Matchers {
  "Mixed rate calculator" should {
    "Calculate first year flat rate, with second year MR" in {
      val fy1Config = FlatRateConfig(2022, 0.19)
      val fy2Config = MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructMixedYear(
        fyFlat = 2022,
        fyMarginal = 2023,
        flatStart = LocalDate.of(2023, 1, 1),
        flatEnd = LocalDate.of(2023, 3, 31),
        marginalStart = LocalDate.of(2023, 4, 1),
        marginalEnd = LocalDate.of(2023, 12, 31),
        daysInAP = 365,
        profit = 100000,
        distributions = 0,
        associatedCompaniesFlat = None,
        associatedCompaniesMarginal = None,
        associatedCompaniesAp = None,
        fYFlatConfig = fy1Config,
        fYMarginalConfig = fy2Config
      )
      val result = MixedRateCalculator.compute(fyData).roundValsUp
      result shouldBe
        DualResult(
          FlatRate(2022, 4684.93, 19.0, 24657.53, 0.0, 24657.53, 90),
          MarginalRate(
            2023,
            18835.62,
            25.0,
            17148.13,
            22.76,
            1687.49,
            75342.47,
            0.0,
            75342.47,
            37568.31,
            187841.53,
            275,
            FYRatio(275, 366)
          ),
          21.83
        )
    }

    "Calculate first year MR with second year flat rate" in {
      val fy1Config = MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
      val fy2Config = FlatRateConfig(2022, 0.19)
      val fyData = FyDataWrapper.constructMixedYear(
        fyFlat = 2023,
        fyMarginal = 2022,
        flatStart = LocalDate.of(2023, 4, 1),
        flatEnd = LocalDate.of(2023, 12, 31),
        marginalStart = LocalDate.of(2023, 1, 1),
        marginalEnd = LocalDate.of(2023, 3, 31),
        daysInAP = 365,
        profit = 100000,
        distributions = 0,
        associatedCompaniesFlat = None,
        associatedCompaniesMarginal = None,
        associatedCompaniesAp = None,
        fYFlatConfig = fy2Config,
        fYMarginalConfig = fy1Config
      )
      val result = MixedRateCalculator.compute(fyData).roundValsUp
      result shouldBe
        DualResult(
          MarginalRate(
            2022,
            6164.38,
            25.00,
            5609.59,
            22.75,
            554.79,
            24657.53,
            0.00,
            24657.53,
            12328.77,
            61643.84,
            90,
            FYRatio(90, 365)
          ),
          FlatRate(2023, 14315.07, 19.00, 75342.47, 0.00, 75342.47, 275),
          19.92
        )
    }
  }
}
