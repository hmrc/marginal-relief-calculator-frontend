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

package models.calculator

import utils.DateUtils
import models.FYConfig

import java.time.LocalDate

case class FyValues[C <: FYConfig](
  fy: Int,
  fYConfig: C,
  apDaysInFY: Int,
  apFYRatio: BigDecimal,
  adjustedProfit: BigDecimal,
  adjustedDistributions: BigDecimal,
  adjustedAugmentedProfit: BigDecimal,
  associatedCompaniesFY: Option[Int]
) {

  def copyWithConcreteConfigType[D <: FYConfig](config: D): FyValues[D] = FyValues[D](
    fy = fy,
    fYConfig = config,
    apDaysInFY = apDaysInFY,
    apFYRatio = apFYRatio,
    adjustedProfit = adjustedProfit,
    adjustedDistributions = adjustedDistributions,
    adjustedAugmentedProfit = adjustedAugmentedProfit,
    associatedCompaniesFY = associatedCompaniesFY
  )
}

object FyValues extends DateUtils {
  def apply[C <: FYConfig](
    fy: Int,
    fyOrApStart: LocalDate,
    fyOrApEnd: LocalDate,
    daysInAp: Int,
    profit: BigDecimal,
    distributions: BigDecimal,
    associatedCompaniesFy: Option[Int],
    fyConfig: C
  ): FyValues[C] = {

    val apDaysInFy = daysBetweenInclusive(fyOrApStart, fyOrApEnd)
    val apFYRatio = BigDecimal(apDaysInFy) / daysInAp
    val adjustedProfitFY = profit * apFYRatio
    val adjustedDistributionsFY = distributions * apFYRatio
    val adjustedAugmentedProfitFY = adjustedProfitFY + adjustedDistributionsFY

    FyValues[C](
      fy = fy,
      fYConfig = fyConfig,
      apDaysInFY = apDaysInFy,
      apFYRatio = apFYRatio,
      adjustedProfit = adjustedProfitFY,
      adjustedDistributions = adjustedDistributionsFY,
      adjustedAugmentedProfit = adjustedAugmentedProfitFY,
      associatedCompaniesFY = associatedCompaniesFy
    )
  }
}
