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
        case SingleResult(marginalRate: MarginalRate, _) => marginalRate.fyRatio shouldBe FYRatio(366, 366)

        case other =>
          fail(s"Unexpected result: $other")
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

    "When AP straddles two FY and adjustedAugmentedProfit equals adjustedLT (7 associated companies)" in {
      val fy1Config = MarginalReliefConfig(2024, 50000, 250000, 0.19, 0.25, 0.015)
      val fy2Config = MarginalReliefConfig(2025, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructDualYear(
        2024,
        2025,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31),
        365,
        LocalDate.of(2025, 3, 31),
        6250,
        0,
        None,
        None,
        Some(7),
        fy1Config,
        fy2Config
      )

      MarginalRateCalculator.computeDouble(fyData).roundValsUp match {
        case DualResult(fy1: MarginalRate, fy2: MarginalRate, effectiveTaxRate) =>
          fy1.taxRateBeforeMR shouldBe 19.0
          fy1.corporationTaxBeforeMR shouldBe 292.81
          fy1.marginalRelief shouldBe 0.0
          fy1.corporationTax shouldBe 292.81

          fy2.taxRateBeforeMR shouldBe 19.0
          fy2.corporationTaxBeforeMR shouldBe 894.69
          fy2.marginalRelief shouldBe 0.0
          fy2.corporationTax shouldBe 894.69

          effectiveTaxRate shouldBe 19.0
        case other => fail(s"Unexpected result: $other")
      }
    }

    "When adjustedAugmentedProfit is a fraction of a penny above adjustedLT, marginal relief still applies" in {
      // adjustedLT = 50000 / 7 = 7142.857142..., adjustedAugmentedProfit = 7142.86
      // Guards against comparing at 2dp, which would wrongly treat these as equal
      val config = MarginalReliefConfig(2025, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructSingleYear(
        2025,
        LocalDate.of(2025, 4, 1),
        365,
        LocalDate.of(2026, 3, 31),
        7142.86,
        0,
        None,
        Some(6),
        config
      )

      MarginalRateCalculator.computeSingle(fyData).roundValsUp match {
        case SingleResult(mr: MarginalRate, _) =>
          mr.taxRateBeforeMR shouldBe 25.0
          mr.corporationTaxBeforeMR shouldBe 1785.72
          mr.marginalRelief shouldBe 428.57
          mr.corporationTax shouldBe 1357.14
        case other => fail(s"Unexpected result: $other")
      }
    }

    "When AP straddles two FY and adjustedAugmentedProfit equals adjustedUT, no marginal relief and main rate applies" in {
      val fy1Config = MarginalReliefConfig(2024, 50000, 250000, 0.19, 0.25, 0.015)
      val fy2Config = MarginalReliefConfig(2025, 50000, 250000, 0.19, 0.25, 0.015)
      val fyData = FyDataWrapper.constructDualYear(
        2024,
        2025,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31),
        365,
        LocalDate.of(2025, 3, 31),
        31250,
        0,
        None,
        None,
        Some(7),
        fy1Config,
        fy2Config
      )

      MarginalRateCalculator.computeDouble(fyData).roundValsUp match {
        case DualResult(fy1: MarginalRate, fy2: MarginalRate, effectiveTaxRate) =>
          fy1.taxRateBeforeMR shouldBe 25.0
          fy1.marginalRelief shouldBe 0.0
          fy1.corporationTax shouldBe 1926.37
          fy2.taxRateBeforeMR shouldBe 25.0
          fy2.marginalRelief shouldBe 0.0
          fy2.corporationTax shouldBe 5886.13
          effectiveTaxRate shouldBe 25.0
        case other => fail(s"Unexpected result: $other")
      }
    }

  }
}
