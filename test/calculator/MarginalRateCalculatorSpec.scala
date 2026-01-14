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

import models.MarginalReliefConfig
import models.calculator.{ DualResult, FYRatio, FyDataWrapper, MarginalRate, SingleResult }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class MarginalRateCalculatorSpec extends AnyWordSpec with Matchers {
  "MarginalRateCalculator" should {
    "when accounting period falls in FY with marginal relief and profits are below the upper threshold" in {
      val config = MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructSingleYear(
        2023,
        LocalDate.of(2023, 4, 1),
        366,
        LocalDate.of(2024, 3, 31),
        100000,
        0,
        None,
        None,
        config
      )
      val result = MarginalRateCalculator.computeSingle(fyData)

      val fYRatio = FYRatio(366, 366)
      val fyRatioJson = FYRatio.format.writes(fYRatio)
      (fyRatioJson \ "numerator").as[Int] shouldBe 366
      (fyRatioJson \ "denominator").as[Int] shouldBe 366

      result shouldBe SingleResult(
        MarginalRate(
          2023,
          25000.0,
          25.0,
          22750.0,
          22.75,
          2250.0,
          100000.0,
          0.0,
          100000.0,
          50000.0,
          250000.0,
          366,
          fYRatio
        ),
        22.75
      )
    }

    "when accounting period falls in a future leap FY, use 366 days when adjusting thresholds" in {
      val config = MarginalReliefConfig(2027, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructSingleYear(
        2027,
        LocalDate.of(2027, 4, 1),
        366,
        LocalDate.of(2028, 3, 31),
        100000,
        0,
        None,
        None,
        config
      )

      val result = MarginalRateCalculator.computeSingle(fyData)
      result match {
        case SingleResult(marginalRate: MarginalRate, _) => marginalRate.fyRatio.shouldBe(FYRatio(366, 366))
      }
    }

    "When accounting period straddles two FY and profits are below upper threshold" in {
      val fy1Config = MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
      val fy2Config = MarginalReliefConfig(2024, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructDualYear(
        2023,
        2024,
        LocalDate.of(2023, 10, 1),
        LocalDate.of(2024, 9, 30),
        366,
        LocalDate.of(2024, 3, 31),
        100000,
        0,
        None,
        None,
        None,
        fy1Config,
        fy2Config
      )
      val result = MarginalRateCalculator.computeDouble(fyData)
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            12500.0,
            25.0,
            11375.0,
            22.75,
            1125.0,
            50000.0,
            0.0,
            50000.0,
            25000.0,
            125000.0,
            183,
            FYRatio(183, 366)
          ),
          MarginalRate(
            2024,
            12500.0,
            25.0,
            11375.0,
            22.75,
            1125.0,
            50000.0,
            0.0,
            50000.0,
            25000.0,
            125000.0,
            183,
            FYRatio(183, 366)
          ),
          22.75
        )
    }
  }
}
