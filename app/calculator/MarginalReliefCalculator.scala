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

import cats.data.ValidatedNel
import cats.syntax.apply._
import com.google.inject.{Inject, Singleton}
import config.{ConfigError, ConfigMissingError, FrontendAppConfig}
import connectors.sharedmodel._
import utils.DateUtils._
import utils.ShowCalculatorDisclaimerUtils.{DateOps, financialYearEnd}

import java.time.LocalDate

@Singleton
class MarginalReliefCalculator @Inject() (appConfig: FrontendAppConfig) {
  
  type CalculatorValidationResult[A] = ValidatedNel[ConfigError, A]

  def compute(accountingPeriodStart: LocalDate,
              accountingPeriodEnd: LocalDate,
              profit: BigDecimal,
              exemptDistributions: BigDecimal,
              associatedCompanies: Option[Int],
              associatedCompaniesFY1: Option[Int],
              associatedCompaniesFY2: Option[Int]): CalculatorValidationResult[CalculatorResult] = {

    def findConfig: Int => CalculatorValidationResult[FYConfig] = appConfig.calculatorConfig.findFYConfig(_)(ConfigMissingError)

    val daysInAP: Int = daysBetweenInclusive(accountingPeriodStart, accountingPeriodEnd)
    val fyEndForAPStartDate: LocalDate = financialYearEnd(accountingPeriodStart)

    if (fyEndForAPStartDate.isEqualOrAfter(accountingPeriodEnd)) {
      // one financial year
      val fy = fyEndForAPStartDate.minusYears(1).getYear
      val maybeFYConfig = findConfig(fy)
      maybeFYConfig.map {
        case flatRateConfig: FlatRateConfig =>
          val ct = BigDecimal(flatRateConfig.mainRate) * profit
          val adjustedAugmentedProfit = profit + exemptDistributions
          val taxRate = roundUp((ct / profit) * 100)
          SingleResult(
            FlatRate(
              fy,
              roundUp(ct),
              taxRate,
              roundUp(profit),
              roundUp(exemptDistributions),
              roundUp(adjustedAugmentedProfit),
              daysInAP
            ),
            taxRate
          )
        case marginalReliefConfig: MarginalReliefConfig =>
          val fyRatioValues = ratioValuesForAdjustingThresholds(
            differentUpperLimitThresholds = false,
            apDaysInFY = daysInAP,
            fyDays = daysInFY(fy),
            daysInAP = daysInAP
          )
          val fyRatio = ratioForAdjustingThresholds(fyRatioValues)
          val companies = associatedCompanies.getOrElse(0) + 1
          val adjustedLT = adjustedThreshold(marginalReliefConfig.lowerThreshold, fyRatio, companies)
          val adjustedUT = adjustedThreshold(marginalReliefConfig.upperThreshold, fyRatio, companies)

          val corporationTaxBeforeMR =
            computeCorporationTax(
              profit,
              profit + exemptDistributions,
              adjustedLT,
              marginalReliefConfig.smallProfitRate,
              marginalReliefConfig.mainRate
            )
          val marginalRelief = computeMarginalRelief(
            profit,
            profit + exemptDistributions,
            adjustedLT,
            adjustedUT,
            marginalReliefConfig.marginalReliefFraction
          )
          val corporationTax = roundUp(corporationTaxBeforeMR - marginalRelief)
          val effectiveRateBeforeMR = roundUp((corporationTaxBeforeMR / profit) * 100)
          val effectiveRate = roundUp((corporationTax / profit) * 100)
          SingleResult(
            MarginalRate(
              fy,
              roundUp(corporationTaxBeforeMR),
              effectiveRateBeforeMR,
              corporationTax,
              effectiveRate,
              roundUp(marginalRelief),
              roundUp(profit),
              roundUp(exemptDistributions),
              roundUp(profit + exemptDistributions),
              roundUp(adjustedLT),
              roundUp(adjustedUT),
              daysInAP,
              fyRatioValues
            ),
            effectiveRate
          )
      }
    } else {
      // straddles financial years
      val fy1 = fyEndForAPStartDate.minusYears(1).getYear
      val fy2 = fyEndForAPStartDate.getYear

      val maybeFY1Config = findConfig(fy1)
      val maybeFY2Config = findConfig(fy2)

      val apDaysInFY1 = daysBetweenInclusive(accountingPeriodStart, fyEndForAPStartDate)
      val apDaysInFY2 = daysInAP - apDaysInFY1

      val apFY1Ratio = BigDecimal(apDaysInFY1) / daysInAP
      val apFY2Ratio = BigDecimal(apDaysInFY2) / daysInAP

      val adjustedProfitFY1 = profit * apFY1Ratio
      val adjustedDistributionsFY1 = exemptDistributions * apFY1Ratio
      val adjustedAugmentedProfitFY1 = adjustedProfitFY1 + adjustedDistributionsFY1

      val adjustedProfitFY2 = profit * apFY2Ratio
      val adjustedDistributionsFY2 = exemptDistributions * apFY2Ratio
      val adjustedAugmentedProfitFY2 = adjustedProfitFY2 + adjustedDistributionsFY2

      (maybeFY1Config, maybeFY2Config).mapN {
        case (fy1Config: FlatRateConfig, fy2Config: FlatRateConfig) =>
          val ctFY1 = BigDecimal(fy1Config.mainRate) * adjustedProfitFY1
          val ctFY2 = BigDecimal(fy2Config.mainRate) * adjustedProfitFY2
          val effectiveTaxRate = ((ctFY1 + ctFY2) / (adjustedProfitFY1 + adjustedProfitFY2)) * 100
          DualResult(
            FlatRate(
              fy1,
              roundUp(ctFY1),
              roundUp((ctFY1 / adjustedProfitFY1) * 100),
              roundUp(adjustedProfitFY1),
              roundUp(adjustedDistributionsFY1),
              roundUp(adjustedProfitFY1 + adjustedDistributionsFY1),
              apDaysInFY1
            ),
            FlatRate(
              fy2,
              roundUp(ctFY2),
              roundUp((ctFY2 / adjustedProfitFY2) * 100),
              roundUp(adjustedProfitFY2),
              roundUp(adjustedDistributionsFY2),
              roundUp(adjustedProfitFY2 + adjustedDistributionsFY2),
              apDaysInFY2
            ),
            roundUp(effectiveTaxRate)
          )
        case (fy1Config: MarginalReliefConfig, fy2Config: MarginalReliefConfig) =>
          val differentUpperLimitThresholds = fy1Config.upperThreshold != fy2Config.upperThreshold
          val fy1RatioValues = ratioValuesForAdjustingThresholds(
            differentUpperLimitThresholds,
            apDaysInFY1,
            daysInFY(fy1),
            daysInAP
          )
          val fy2RatioValues = ratioValuesForAdjustingThresholds(
            differentUpperLimitThresholds,
            apDaysInFY2,
            daysInFY(fy2),
            daysInAP
          )

          val fy1Ratio = ratioForAdjustingThresholds(fy1RatioValues)
          val fy2Ratio = ratioForAdjustingThresholds(fy2RatioValues)

          val companiesFY1 = calculateCompanies(
            fy1Config,
            fy2Config,
            associatedCompanies,
            associatedCompaniesFY1,
            associatedCompaniesFY2,
            fy1Config.year
          )
          val companiesFY2 = calculateCompanies(
            fy1Config,
            fy2Config,
            associatedCompanies,
            associatedCompaniesFY1,
            associatedCompaniesFY2,
            fy2Config.year
          )

          val adjustedLTFY1 = adjustedThreshold(fy1Config.lowerThreshold, fy1Ratio, companiesFY1)
          val adjustedUTFY1 = adjustedThreshold(fy1Config.upperThreshold, fy1Ratio, companiesFY1)

          val adjustedLTFY2 = adjustedThreshold(fy2Config.lowerThreshold, fy2Ratio, companiesFY2)
          val adjustedUTFY2 = adjustedThreshold(fy2Config.upperThreshold, fy2Ratio, companiesFY2)

          val ctFY1 =
            computeCorporationTax(
              adjustedProfitFY1,
              adjustedAugmentedProfitFY1,
              adjustedLTFY1,
              fy1Config.smallProfitRate,
              fy1Config.mainRate
            )
          val ctFY2 =
            computeCorporationTax(
              adjustedProfitFY2,
              adjustedAugmentedProfitFY2,
              adjustedLTFY2,
              fy2Config.smallProfitRate,
              fy2Config.mainRate
            )
          val mr1 = computeMarginalRelief(
            adjustedProfitFY1,
            adjustedAugmentedProfitFY1,
            adjustedLTFY1,
            adjustedUTFY1,
            fy1Config.marginalReliefFraction
          )
          val mr2 = computeMarginalRelief(
            adjustedProfitFY2,
            adjustedAugmentedProfitFY2,
            adjustedLTFY2,
            adjustedUTFY2,
            fy2Config.marginalReliefFraction
          )

          val effectiveTaxRate = ((ctFY1 - mr1 + ctFY2 - mr2) / (adjustedProfitFY1 + adjustedProfitFY2)) * 100

          DualResult(
            MarginalRate(
              fy1,
              roundUp(ctFY1),
              roundUp((ctFY1 / adjustedProfitFY1) * 100),
              roundUp(ctFY1 - mr1),
              roundUp(((ctFY1 - mr1) / adjustedProfitFY1) * 100),
              roundUp(mr1),
              roundUp(adjustedProfitFY1),
              roundUp(adjustedDistributionsFY1),
              roundUp(adjustedProfitFY1 + adjustedDistributionsFY1),
              roundUp(adjustedLTFY1),
              roundUp(adjustedUTFY1),
              apDaysInFY1,
              fy1RatioValues
            ),
            MarginalRate(
              fy2,
              roundUp(ctFY2),
              roundUp((ctFY2 / adjustedProfitFY2) * 100),
              roundUp(ctFY2 - mr2),
              roundUp(((ctFY2 - mr2) / adjustedProfitFY2) * 100),
              roundUp(mr2),
              roundUp(adjustedProfitFY2),
              roundUp(adjustedDistributionsFY2),
              roundUp(adjustedProfitFY2 + adjustedDistributionsFY2),
              roundUp(adjustedLTFY2),
              roundUp(adjustedUTFY2),
              apDaysInFY2,
              fy2RatioValues
            ),
            roundUp(effectiveTaxRate)
          )
        case (fy1Config: FlatRateConfig, fy2Config: MarginalReliefConfig) =>
          val ctFY1 = BigDecimal(fy1Config.mainRate) * adjustedProfitFY1
          val fy2RatioValues = ratioValuesForAdjustingThresholds(
            differentUpperLimitThresholds = true,
            apDaysInFY2,
            daysInFY(fy2),
            daysInAP
          )

          val fy2Ratio = ratioForAdjustingThresholds(fy2RatioValues)

          val companiesFY2 = associatedCompanies.map(_ + 1).orElse(associatedCompaniesFY2.map(_ + 1)).getOrElse(1)
          val adjustedLTFY2 = adjustedThreshold(fy2Config.lowerThreshold, fy2Ratio, companiesFY2)
          val adjustedUTFY2 = adjustedThreshold(fy2Config.upperThreshold, fy2Ratio, companiesFY2)

          val ctFY2 =
            computeCorporationTax(
              adjustedProfitFY2,
              adjustedAugmentedProfitFY2,
              adjustedLTFY2,
              fy2Config.smallProfitRate,
              fy2Config.mainRate
            )

          val mr2 = computeMarginalRelief(
            adjustedProfitFY2,
            adjustedAugmentedProfitFY2,
            adjustedLTFY2,
            adjustedUTFY2,
            fy2Config.marginalReliefFraction
          )

          val effectiveTaxRate = ((ctFY1 + ctFY2 - mr2) / (adjustedProfitFY1 + adjustedProfitFY2)) * 100

          DualResult(
            FlatRate(
              fy1,
              roundUp(ctFY1),
              roundUp((ctFY1 / adjustedProfitFY1) * 100),
              roundUp(adjustedProfitFY1),
              roundUp(adjustedDistributionsFY1),
              roundUp(adjustedProfitFY1 + adjustedDistributionsFY1),
              apDaysInFY1
            ),
            MarginalRate(
              fy2,
              roundUp(ctFY2),
              roundUp((ctFY2 / adjustedProfitFY2) * 100),
              roundUp(ctFY2 - mr2),
              roundUp(((ctFY2 - mr2) / adjustedProfitFY2) * 100),
              roundUp(mr2),
              roundUp(adjustedProfitFY2),
              roundUp(adjustedDistributionsFY2),
              roundUp(adjustedProfitFY2 + adjustedDistributionsFY2),
              roundUp(adjustedLTFY2),
              roundUp(adjustedUTFY2),
              apDaysInFY2,
              fy2RatioValues
            ),
            roundUp(effectiveTaxRate)
          )
        case (fy1Config: MarginalReliefConfig, fy2Config: FlatRateConfig) =>
          val fy1RatioValues = ratioValuesForAdjustingThresholds(
            differentUpperLimitThresholds = true,
            apDaysInFY1,
            daysInFY(fy1),
            daysInAP
          )

          val fy1Ratio = ratioForAdjustingThresholds(fy1RatioValues)

          val companiesFY1 = associatedCompanies.map(_ + 1).orElse(associatedCompaniesFY1.map(_ + 1)).getOrElse(1)
          val adjustedLTFY1 = adjustedThreshold(fy1Config.lowerThreshold, fy1Ratio, companiesFY1)
          val adjustedUTFY1 = adjustedThreshold(fy1Config.upperThreshold, fy1Ratio, companiesFY1)

          val ctFY1 =
            computeCorporationTax(
              adjustedProfitFY1,
              adjustedAugmentedProfitFY1,
              adjustedLTFY1,
              fy1Config.smallProfitRate,
              fy1Config.mainRate
            )
          val mr1 = computeMarginalRelief(
            adjustedProfitFY1,
            adjustedAugmentedProfitFY1,
            adjustedLTFY1,
            adjustedUTFY1,
            fy1Config.marginalReliefFraction
          )
          val ctFY2 = BigDecimal(fy2Config.mainRate) * adjustedProfitFY2

          val effectiveTaxRate = ((ctFY1 - mr1 + ctFY2) / (adjustedProfitFY1 + adjustedProfitFY2)) * 100

          DualResult(
            MarginalRate(
              fy1,
              roundUp(ctFY1),
              roundUp((ctFY1 / adjustedProfitFY1) * 100),
              roundUp(ctFY1 - mr1),
              roundUp(((ctFY1 - mr1) / adjustedProfitFY1) * 100),
              roundUp(mr1),
              roundUp(adjustedProfitFY1),
              roundUp(adjustedDistributionsFY1),
              roundUp(adjustedProfitFY1 + adjustedDistributionsFY1),
              roundUp(adjustedLTFY1),
              roundUp(adjustedUTFY1),
              apDaysInFY1,
              fy1RatioValues
            ),
            FlatRate(
              fy2,
              roundUp(ctFY2),
              roundUp((ctFY2 / adjustedProfitFY2) * 100),
              roundUp(adjustedProfitFY2),
              roundUp(adjustedDistributionsFY2),
              roundUp(adjustedProfitFY2 + adjustedDistributionsFY2),
              apDaysInFY2
            ),
            roundUp(effectiveTaxRate)
          )
      }
    }
  }

  def calculateCompanies(
    fy1Config: MarginalReliefConfig,
    fy2Config: MarginalReliefConfig,
    maybeAssociatedCompanies: Option[Int],
    maybeAssociatedCompaniesFY1: Option[Int],
    maybeAssociatedCompaniesFY2: Option[Int],
    forFYYear: Int
  ): Int =
    maybeAssociatedCompanies match {
      case Some(associatedCompanies) => associatedCompanies + 1
      case None =>
        (maybeAssociatedCompaniesFY1, maybeAssociatedCompaniesFY2) match {
          case (Some(associatedCompaniesFY1), None) => associatedCompaniesFY1 + 1
          case (None, Some(associatedCompaniesFY2)) => associatedCompaniesFY2 + 1
          case (Some(associatedCompaniesFY1), Some(associatedCompaniesFY2))
              if fy1Config.lowerThreshold == fy2Config.lowerThreshold && fy1Config.upperThreshold == fy2Config.upperThreshold =>
            Math.max(associatedCompaniesFY1, associatedCompaniesFY2) + 1
          case (Some(associatedCompaniesFY1), Some(associatedCompaniesFY2)) =>
            if (fy1Config.year == forFYYear) associatedCompaniesFY1 + 1 else associatedCompaniesFY2 + 1
          case (None, None) => 1
        }
    }

  private def computeCorporationTax(
    adjustedProfit: BigDecimal,
    adjustedAugmentedProfit: BigDecimal,
    adjustedLT: BigDecimal,
    smallProfitRate: Double,
    mainRate: Double
  ): BigDecimal = {
    // calculate corporation tax
    val corporationTax =
      adjustedProfit * (if (adjustedAugmentedProfit <= adjustedLT) BigDecimal(smallProfitRate)
                        else BigDecimal(mainRate))
    corporationTax
  }

  private def computeMarginalRelief(
    adjustedProfit: BigDecimal,
    adjustedAugmentedProfit: BigDecimal,
    adjustedLT: BigDecimal,
    adjustedUT: BigDecimal,
    marginalReliefFraction: Double
  ): BigDecimal =
    // calculate marginal relief
    if (adjustedAugmentedProfit > adjustedLT && adjustedAugmentedProfit <= adjustedUT) {
      BigDecimal(
        marginalReliefFraction
      ) * (adjustedUT - adjustedAugmentedProfit) * (adjustedProfit / adjustedAugmentedProfit)
    } else {
      BigDecimal(0)
    }

  case class UpperThresholds(upperThresholdFY1: Option[Int], upperThresholdFY2: Option[Int])

  private def ratioValuesForAdjustingThresholds(
    differentUpperLimitThresholds: Boolean,
    apDaysInFY: Int,
    fyDays: Int,
    daysInAP: Int
  ): FYRatio = {
    val totalNoOfDays = if (differentUpperLimitThresholds) fyDays else 365 max daysInAP
    FYRatio(BigDecimal(apDaysInFY), totalNoOfDays)
  }

  private def ratioForAdjustingThresholds(values: FYRatio): BigDecimal = values.numerator / values.denominator

  private def adjustedThreshold(threshold: Int, fyRatio: BigDecimal, companies: Int): BigDecimal =
    (threshold * fyRatio) / BigDecimal(companies)

  private def roundUp(value: BigDecimal): Double =
    value.setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
}
