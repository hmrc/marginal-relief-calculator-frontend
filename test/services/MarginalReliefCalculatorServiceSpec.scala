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

import cats.data.NonEmptyList
import cats.data.Validated.Invalid
import cats.syntax.validated.catsSyntaxValidatedId
import com.typesafe.config.ConfigFactory
import models.calculator._
import config.{ ConfigMissingError, FrontendAppConfig }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

import java.time.LocalDate

class MarginalReliefCalculatorServiceSpec extends AnyWordSpec with Matchers {

  "compute" when {

    "config missing" should {

      "when single year and config missing, return error" in {
        val marginalReliefCalculator = new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                                              |appName = test
                                                                                              |calculator-config = {
                                                                                              | fy-configs = [
                                                                                              | ]
                                                                                              |}
                                                                                              |""".stripMargin))
        val result = marginalReliefCalculator.compute(
          LocalDate.of(2022, 4, 1),
          LocalDate.of(2023, 3, 31),
          1,
          0,
          None,
          None,
          None
        )
        result shouldBe ConfigMissingError(2022).invalidNel
      }

      "when straddles two financial years and both year configs missing, return error" in {
        val marginalReliefCalculator = new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                                              |appName = test
                                                                                              |calculator-config = {
                                                                                              | fy-configs = []
                                                                                              |}
                                                                                              |""".stripMargin))
        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 1, 1),
          LocalDate.of(2023, 12, 31),
          1,
          0,
          None,
          None,
          None
        )
        result shouldBe Invalid(
          NonEmptyList.of(ConfigMissingError(2022), ConfigMissingError(2023))
        )
      }
    }

    "accounting period falls in a single financial year" should {

      "when config is missing for an accounting period year, fallback to the nearest config" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.1
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     main-rate = 0.2
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))
        val result = marginalReliefCalculator.compute(
          LocalDate.of(2024, 4, 1),
          LocalDate.of(2025, 3, 31),
          100000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(FlatRate(2024, 20000.0, 20.0, 100000, 0, 100000, 365), 20).valid
      }

      "when account period falls in FY with only main rate, apply main rate with no marginal relief" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.19
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))
        val result = marginalReliefCalculator.compute(
          LocalDate.of(2022, 4, 1),
          LocalDate.of(2023, 3, 31),
          100000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(FlatRate(2022, 19000.0, 19.0, 100000, 0, 100000, 365), 19).valid
      }

      "when a shorter account period falls in a single FY with MR and profits are above the upper threshold, apply main rate and MR ratio with 365 days" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 5, 1),
          LocalDate.of(2024, 3, 31),
          300000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(
            2023,
            75000.0,
            25.0,
            75000.0,
            25.0,
            0.0,
            300000.0,
            0.0,
            300000.0,
            46027.4,
            230136.99,
            336,
            FYRatio(336, 365)
          ),
          25
        ).valid
      }

      "when account period falls in FY with marginal relief and profits are above the upper threshold, apply main rate" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 4, 1),
          LocalDate.of(2024, 3, 31),
          300000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(
            2023,
            75000.0,
            25.0,
            75000.0,
            25.0,
            0.0,
            300000.0,
            0.0,
            300000.0,
            50000.0,
            250000.0,
            366,
            FYRatio(366, 366)
          ),
          25
        ).valid
      }

      "when account period falls in FY with marginal relief and profits are matching lower threshold, apply small profits rate" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 4, 1),
          LocalDate.of(2024, 3, 31),
          50000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(
            2023,
            9500.0,
            19.0,
            9500.0,
            19.0,
            0.0,
            50000.0,
            0.0,
            50000,
            50000.0,
            250000.0,
            366,
            FYRatio(366, 366)
          ),
          19
        ).valid
      }

      "when account period falls in FY with marginal relief and profits are below lower threshold, apply small profits rate" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 4, 1),
          LocalDate.of(2024, 3, 31),
          40000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(2023, 7600.0, 19.0, 7600.0, 19.0, 0, 40000, 0, 40000, 50000, 250000, 366, FYRatio(366, 366)),
          19
        ).valid
      }
      "when account period falls in FY with marginal relief and profits are between upper and lower thresholds, apply main rate with marginal relief" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 4, 1),
          LocalDate.of(2024, 3, 31),
          100000,
          0,
          None,
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(
            2023,
            25000.0,
            25.0,
            22750.0,
            22.75,
            2250.0,
            100000,
            0,
            100000,
            50000,
            250000,
            366,
            FYRatio(366, 366)
          ),
          22.75
        ).valid
      }

      "when account period falls in FY with marginal relief and profits are between upper and lower thresholds and there are associated companies, apply main rate with marginal relief" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))
        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 4, 1),
          LocalDate.of(2024, 3, 31),
          100000,
          0,
          Some(1),
          None,
          None
        )
        result shouldBe SingleResult(
          MarginalRate(
            2023,
            25000.0,
            25.0,
            24625.0,
            24.63,
            375.0,
            100000.0,
            0.0,
            100000,
            25000.0,
            125000.0,
            366,
            FYRatio(366, 366)
          ),
          24.63
        ).valid
      }
    }
    "accounting period straddles financial period" should {

      "when config is empty, return error" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 1, 1),
          LocalDate.of(2023, 12, 31),
          10000,
          0,
          None,
          None,
          None
        )
        result shouldBe Invalid(
          NonEmptyList.of(ConfigMissingError(2022), ConfigMissingError(2023))
        )
      }

      "when config missing for one of the years, fallback to the nearest config for the missing year" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.19
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2024, 1, 1),
          LocalDate.of(2024, 12, 31),
          10000,
          0,
          None,
          None,
          None
        )
        result shouldBe
          DualResult(
            MarginalRate(
              2023,
              472.4,
              19.0,
              472.4,
              19.0,
              0.0,
              2486.34,
              0.0,
              2486.34,
              12431.69,
              62158.47,
              91,
              FYRatio(91, 366)
            ),
            MarginalRate(
              2024,
              1427.6,
              19.0,
              1427.6,
              19.0,
              0.0,
              7513.66,
              0.0,
              7513.66,
              37568.31,
              187841.53,
              275,
              FYRatio(275, 366)
            ),
            19
          ).valid
      }

      "when config missing for both years, fallback to the nearest config" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.19
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2025, 1, 1),
          LocalDate.of(2025, 12, 31),
          10000,
          0,
          None,
          None,
          None
        )
        result shouldBe DualResult(
          MarginalRate(
            2024,
            468.49,
            19.0,
            468.49,
            19.0,
            0.0,
            2465.75,
            0.0,
            2465.75,
            12328.77,
            61643.84,
            90,
            FYRatio(90, 365)
          ),
          MarginalRate(
            2025,
            1431.51,
            19.0,
            1431.51,
            19.0,
            0.0,
            7534.25,
            0.0,
            7534.25,
            37671.23,
            188356.16,
            275,
            FYRatio(275, 365)
          ),
          19
        ).valid
      }

      "when both years are flat rate, calculate corporation tax with no marginal relief" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.19
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     main-rate = 0.20
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 1, 1),
          LocalDate.of(2023, 12, 31),
          100000,
          0,
          None,
          None,
          None
        )
        result shouldBe
          DualResult(
            FlatRate(2022, 4684.93, 19.0, 24657.53, 0, 24657.53, 90),
            FlatRate(2023, 15068.49, 20.0, 75342.47, 0, 75342.47, 275),
            19.75
          ).valid

      }

      "when no associated companies throughout the accounting period, no change in rates or thresholds" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2024
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2024, 1, 1),
          LocalDate.of(2024, 6, 30),
          25000,
          0,
          None,
          None,
          None
        )
        result shouldBe
          DualResult(
            MarginalRate(
              2023,
              3125.0,
              25.0,
              2377.57,
              19.02,
              747.43,
              12500.0,
              0.0,
              12500.0,
              12465.75,
              62328.77,
              91,
              FYRatio(91, 365)
            ),
            MarginalRate(
              2024,
              3125.0,
              25.0,
              2377.57,
              19.02,
              747.43,
              12500.0,
              0.0,
              12500.0,
              12465.75,
              62328.77,
              91,
              FYRatio(91, 365)
            ),
            19.02
          ).valid

      }
      "when FY1 with MR rate and FY2 with flat rate and associated companies in FY1, profits within threshold for FY1" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2024
                                                                 |     main-rate = 0.19
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 6, 1),
          LocalDate.of(2024, 5, 31),
          70000,
          0,
          None,
          Some(2),
          None
        )
        result shouldBe
          DualResult(
            MarginalRate(
              2023,
              14583.33,
              25.0,
              14416.67,
              24.71,
              166.67,
              58333.33,
              0.0,
              58333.33,
              13888.89,
              69444.44,
              305,
              FYRatio(305, 366)
            ),
            FlatRate(2024, 2216.67, 19.0, 11666.67, 0, 11666.67, 61),
            23.76
          ).valid
      }
      "when FY1 with MR rate and FY2 with MR rate (no change in config), associated companies in (FY1, FY2) and profits within thresholds" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2024
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 10, 1),
          LocalDate.of(2024, 9, 30),
          60000,
          0,
          None,
          Some(2),
          Some(3)
        )
        result shouldBe
          DualResult(
            MarginalRate(
              2023,
              7500.0,
              25.0,
              7481.25,
              24.94,
              18.75,
              30000.0,
              0.0,
              30000,
              6250.0,
              31250.0,
              183,
              FYRatio(183, 366)
            ),
            MarginalRate(
              2024,
              7500.0,
              25.0,
              7481.25,
              24.94,
              18.75,
              30000.0,
              0.0,
              30000,
              6250.0,
              31250.0,
              183,
              FYRatio(183, 366)
            ),
            24.94
          ).valid

      }
      "when FY1 with MR rate and FY2 with MR rate (change in config), associated companies in (FY1, FY2) and profits within thresholds" in {
        // Calculation for a company with short AP
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 |   {
                                                                 |     year = 2024
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 300000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   },
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 10, 1),
          LocalDate.of(2024, 9, 30),
          60000,
          0,
          None,
          Some(2),
          Some(3)
        )
        result shouldBe
          DualResult(
            MarginalRate(
              2023,
              7500.0,
              25.0,
              7325.0,
              24.42,
              175.0,
              30000.0,
              0.0,
              30000,
              8333.33,
              41666.67,
              183,
              FYRatio(183, 366)
            ),
            MarginalRate(
              2024,
              7500.0,
              25.0,
              7385.96,
              24.62,
              114.04,
              30000.0,
              0.0,
              30000,
              6267.12,
              37602.74,
              183,
              FYRatio(183, 365)
            ),
            24.52
          ).valid

      }
      "when 2 associated companies in second accounting period, FY1 with flat rate and FY2 with MR rate - FY2 profits above upper threshold" in {
        val marginalReliefCalculator =
          new MarginalReliefCalculatorService(appConfigFromStr("""
                                                                 |appName = test
                                                                 |calculator-config = {
                                                                 | fy-configs = [
                                                                 |   {
                                                                 |     year = 2022
                                                                 |     main-rate = 0.19
                                                                 |   }
                                                                 |   {
                                                                 |     year = 2023
                                                                 |     lower-threshold = 50000
                                                                 |     upper-threshold = 250000
                                                                 |     small-profit-rate = 0.19
                                                                 |     main-rate = 0.25
                                                                 |     marginal-relief-fraction = 0.015
                                                                 |   }
                                                                 | ]
                                                                 |}
                                                                 |""".stripMargin))

        val result = marginalReliefCalculator.compute(
          LocalDate.of(2023, 1, 1),
          LocalDate.of(2023, 12, 31),
          175000,
          0,
          None,
          None,
          Some(2)
        )
        result shouldBe
          DualResult(
            FlatRate(2022, 8198.63, 19.0, 43150.68, 0, 43150.68, 90),
            MarginalRate(
              2023,
              32962.33,
              25.0,
              32962.33,
              25.0,
              0.0,
              131849.32,
              0.0,
              131849.32,
              12522.77,
              62613.84,
              275,
              FYRatio(275, 366)
            ),
            23.52
          ).valid
      }
    }

    "when 2 associated in the first accounting period, FY1 with MR rate and FY2 with flat rate - FY1 profits between thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     main-rate = 0.19
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 31),
        50000,
        10000,
        None,
        Some(2),
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            3107.92,
            25.0,
            3035.41,
            24.42,
            72.52,
            12431.69,
            2486.34,
            14918.03,
            4143.9,
            20719.49,
            91,
            FYRatio(91, 366)
          ),
          FlatRate(2024, 7137.98, 19.0, 37568.31, 7513.66, 45081.97, 275),
          20.35
        ).valid
    }

    "when 2 associated in the second accounting period, FY1 with flat rate and FY2 with MR rate - FY2 profits between thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2022
                                                               |     main-rate = 0.19
                                                               |   }
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 12, 31),
        60000,
        0,
        None,
        None,
        Some(2)
      )
      result shouldBe
        DualResult(
          FlatRate(2022, 2810.96, 19.0, 14794.52, 0, 14794.52, 90),
          MarginalRate(
            2023,
            11301.37,
            25.0,
            11040.24,
            24.42,
            261.13,
            45205.48,
            0.0,
            45205.48,
            12522.77,
            62613.84,
            275,
            FYRatio(275, 366)
          ),
          23.09
        ).valid
    }

    "when 1 associated for the whole accounting period, FY1 with flat rate and FY2 with MR rate - FY2 profits between thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2022
                                                               |     main-rate = 0.19
                                                               |   }
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 12, 31),
        60000,
        0,
        Some(2),
        None,
        None
      )
      result shouldBe
        DualResult(
          FlatRate(2022, 2810.96, 19.0, 14794.52, 0.0, 14794.52, 90),
          MarginalRate(
            2023,
            11301.37,
            25.0,
            11040.24,
            24.42,
            261.13,
            45205.48,
            0.0,
            45205.48,
            12522.77,
            62613.84,
            275,
            FYRatio(275, 366)
          ),
          23.09
        ).valid
    }

    "when 1 associated for the whole accounting period, FY1 with MR rate and FY2 with flat rate - FY1 profits between thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     main-rate = 0.19
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 31),
        50000,
        10000,
        Some(1),
        None,
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            3107.92,
            25.0,
            2905.91,
            23.38,
            202.02,
            12431.69,
            2486.34,
            14918.03,
            6215.85,
            31079.23,
            91,
            FYRatio(91, 366)
          ),
          FlatRate(2024, 7137.98, 19.0, 37568.31, 7513.66, 45081.97, 275),
          20.09
        ).valid
    }

    "when no of associated companies changes, no change in rates or thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2023, 10, 1),
        LocalDate.of(2024, 9, 30),
        55000,
        0,
        Some(3),
        None,
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            6875.0,
            25.0,
            6818.75,
            24.8,
            56.25,
            27500.0,
            0.0,
            27500.0,
            6250.0,
            31250.0,
            183,
            FYRatio(183, 366)
          ),
          MarginalRate(
            2024,
            6875.0,
            25.0,
            6818.75,
            24.8,
            56.25,
            27500.0,
            0.0,
            27500.0,
            6250.0,
            31250.0,
            183,
            FYRatio(183, 366)
          ),
          24.8
        ).valid

    }
    "when no of associated companies and rates change, no change in thresholds" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2027
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2028
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.20
                                                               |     main-rate = 0.26
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2028, 1, 1),
        LocalDate.of(2028, 12, 31),
        45000,
        0,
        Some(4),
        None,
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2027,
            2797.13,
            25.0,
            2778.48,
            24.83,
            18.65,
            11188.52,
            0.0,
            11188.52,
            2486.34,
            12431.69,
            91,
            FYRatio(91, 366)
          ),
          MarginalRate(
            2028,
            8790.98,
            26.0,
            8734.63,
            25.83,
            56.35,
            33811.48,
            0.0,
            33811.48,
            7513.66,
            37568.31,
            275,
            FYRatio(275, 366)
          ),
          25.58
        ).valid

    }
    "when no of associated companies and thresholds change, no change in rates" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2030
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2031
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 300000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.012
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2030, 7, 1),
        LocalDate.of(2031, 6, 30),
        85000,
        0,
        None,
        Some(2),
        Some(1)
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2030,
            15952.05,
            25.0,
            15952.05,
            25.0,
            0.0,
            63808.22,
            0.0,
            63808.22,
            12511.42,
            62557.08,
            274,
            FYRatio(274, 365)
          ),
          MarginalRate(
            2031,
            5297.95,
            25.0,
            5104.71,
            24.09,
            193.24,
            21191.78,
            0.0,
            21191.78,
            6215.85,
            37295.08,
            91,
            FYRatio(91, 366)
          ),
          24.77
        ).valid

    }
    "when rates change, no change in thresholds and accounting period is 365 days" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.26
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 30),
        100000,
        0,
        None,
        None,
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            6232.88,
            25.0,
            5671.92,
            22.75,
            560.96,
            24931.51,
            0.0,
            24931.51,
            12465.75,
            62328.77,
            91,
            FYRatio(91, 365)
          ),
          MarginalRate(
            2024,
            19517.81,
            26.0,
            17828.77,
            23.75,
            1689.04,
            75068.49,
            0.0,
            75068.49,
            37534.25,
            187671.23,
            274,
            FYRatio(274, 365)
          ),
          23.5
        ).valid
    }
    "when associated companies in FY1 and no associated companies in FY2, FY1 and FY2 are MR rates" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 30),
        100000,
        0,
        None,
        Some(1),
        None
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            6232.88,
            25.0,
            6139.38,
            24.63,
            93.49,
            24931.51,
            0.0,
            24931.51,
            6232.88,
            31164.38,
            91,
            FYRatio(91, 365)
          ),
          MarginalRate(
            2024,
            18767.12,
            25.0,
            18485.62,
            24.63,
            281.51,
            75068.49,
            0.0,
            75068.49,
            18767.12,
            93835.62,
            274,
            FYRatio(274, 365)
          ),
          24.63
        ).valid
    }
    "when no associated companies in FY1 and associated companies in FY2, FY1 and FY2 are MR rates" in {
      val marginalReliefCalculator =
        new MarginalReliefCalculatorService(appConfigFromStr("""
                                                               |appName = test
                                                               |calculator-config = {
                                                               | fy-configs = [
                                                               |   {
                                                               |     year = 2023
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   },
                                                               |   {
                                                               |     year = 2024
                                                               |     lower-threshold = 50000
                                                               |     upper-threshold = 250000
                                                               |     small-profit-rate = 0.19
                                                               |     main-rate = 0.25
                                                               |     marginal-relief-fraction = 0.015
                                                               |   }
                                                               | ]
                                                               |}
                                                               |""".stripMargin))

      val result = marginalReliefCalculator.compute(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 30),
        100000,
        0,
        None,
        None,
        Some(1)
      )
      result shouldBe
        DualResult(
          MarginalRate(
            2023,
            6232.88,
            25.0,
            6139.38,
            24.63,
            93.49,
            24931.51,
            0.0,
            24931.51,
            6232.88,
            31164.38,
            91,
            FYRatio(91, 365)
          ),
          MarginalRate(
            2024,
            18767.12,
            25.0,
            18485.62,
            24.63,
            281.51,
            75068.49,
            0.0,
            75068.49,
            18767.12,
            93835.62,
            274,
            FYRatio(274, 365)
          ),
          24.63
        ).valid
    }
  }

  def appConfigFromStr(configStr: String): FrontendAppConfig =
    new FrontendAppConfig(Configuration(ConfigFactory.parseString(configStr).withFallback(ConfigFactory.load())))
}
