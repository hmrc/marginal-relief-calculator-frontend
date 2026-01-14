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

package calculator.computations

import models.MarginalReliefConfig
import models.calculator.FyValues.daysInFY
import models.calculator.{ FYRatio, FyValues, MarginalRate, SingleResult }

trait MarginalRateComputation {

  def ratioValuesForAdjustingThresholds(
    differentUpperLimitThresholds: Boolean,
    apDaysInFY: Int,
    fyDays: Int,
    daysInAP: Int
  ): FYRatio = {
    val totalNoOfDays = if (differentUpperLimitThresholds) fyDays else fyDays max daysInAP
    FYRatio(BigDecimal(apDaysInFY), totalNoOfDays)
  }

  def adjustedThreshold(threshold: Int, fyRatio: BigDecimal, companies: Int): BigDecimal =
    (threshold * fyRatio) / BigDecimal(companies)

  def computeMarginalRateForFy(
    fyValues: FyValues[MarginalReliefConfig],
    associatedCompanies: Int,
    daysInAp: Int,
    differentUpperLimitThresholds: Boolean
  ): SingleResult[MarginalRate] = {

    import fyValues._
    import fyValues.fYConfig.{ mainRate, marginalReliefFraction, smallProfitRate }

    val fyRatio: FYRatio = ratioValuesForAdjustingThresholds(
      differentUpperLimitThresholds = differentUpperLimitThresholds,
      apDaysInFY = apDaysInFY,
      fyDays = daysInFY(fy),
      daysInAP = daysInAp
    )

    val adjustedLT: BigDecimal = adjustedThreshold(
      threshold = fYConfig.lowerThreshold,
      fyRatio = fyRatio.ratio,
      companies = associatedCompanies
    )

    val adjustedUT: BigDecimal = adjustedThreshold(
      threshold = fYConfig.upperThreshold,
      fyRatio = fyRatio.ratio,
      companies = associatedCompanies
    )

    val ctBeforeMarginalRelief = adjustedProfit * (if (adjustedAugmentedProfit <= adjustedLT) {
                                                     BigDecimal(smallProfitRate)
                                                   } else {
                                                     BigDecimal(mainRate)
                                                   })

    val marginalRelief = if (adjustedAugmentedProfit > adjustedLT && adjustedAugmentedProfit <= adjustedUT) {
      BigDecimal(marginalReliefFraction) *
        (adjustedUT - adjustedAugmentedProfit) *
        (adjustedProfit / adjustedAugmentedProfit)
    } else {
      BigDecimal(0)
    }

    val effectiveRateBeforeMR = (ctBeforeMarginalRelief / adjustedProfit) * 100

    val corporationTax: BigDecimal = ctBeforeMarginalRelief - marginalRelief
    val effectiveRate = (corporationTax / adjustedProfit) * 100

    SingleResult[MarginalRate](
      taxDetails = MarginalRate(
        year = fy,
        corporationTaxBeforeMR = ctBeforeMarginalRelief,
        taxRateBeforeMR = effectiveRateBeforeMR,
        corporationTax = corporationTax,
        taxRate = effectiveRate,
        marginalRelief = marginalRelief,
        adjustedProfit = adjustedProfit,
        adjustedDistributions = adjustedDistributions,
        adjustedAugmentedProfit = adjustedProfit + adjustedDistributions,
        adjustedLowerThreshold = adjustedLT,
        adjustedUpperThreshold = adjustedUT,
        days = apDaysInFY,
        fyRatio = fyRatio
      ),
      effectiveTaxRate = effectiveRate
    )
  }
}
