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

import models.FlatRateConfig
import models.calculator.{ DualResult, FlatRate, FyDataWrapper, SingleResult }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class FlatRateCalculatorSpec extends AnyWordSpec with Matchers {
  "flat rate calculator spec" should {
    "apply main rate with no marginal relief when account period falls in FY with only main rate" in {
      val config = FlatRateConfig(2022, 0.19)
      val fyData = FyDataWrapper.constructSingleYear[FlatRateConfig](
        2022,
        LocalDate.of(2022, 4, 1),
        365,
        LocalDate.of(2023, 3, 31),
        100000,
        0,
        None,
        None,
        config
      )
      val result = FlatRateCalculator.computeSingle(fyData)
      result shouldBe SingleResult(FlatRate(2022, 19000.0, 19.0, 100000, 0, 100000, 365), 19)
    }
    "calculate corporation tax with no marginal relief when both years are flat rate" in {
      val y1Config = FlatRateConfig(2022, 0.19)
      val y2Config = FlatRateConfig(2023, 0.20)
      val fyData = FyDataWrapper.constructDualYear(
        2022,
        2023,
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 12, 31),
        365,
        LocalDate.of(2023, 3, 31),
        100000,
        0,
        None,
        None,
        None,
        y1Config,
        y2Config
      )
      val result = FlatRateCalculator.computeDouble(fyData).roundValsUp
      result shouldBe
        DualResult(
          FlatRate(2022, 4684.93, 19.0, 24657.53, 0, 24657.53, 90),
          FlatRate(2023, 15068.49, 20.0, 75342.47, 0, 75342.47, 275),
          19.75
        )
    }
  }
}
