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

import calculator.computations.MarginalRateComputation
import models.MarginalReliefConfig
import models.calculator._

object MarginalRateCalculator extends MarginalRateComputation {

  def computeSingle(fyDataWrapper: SingleFyDataWrapper[MarginalReliefConfig]): CalculatorResult = {
    val associatedCompanies: Int = fyDataWrapper.associatedCompaniesAp.getOrElse(
      fyDataWrapper.fy1Values.associatedCompaniesFY.getOrElse(0)
    ) + 1

    computeMarginalRateForFy(
      fyValues = fyDataWrapper.fy1Values,
      associatedCompanies = associatedCompanies,
      daysInAp = fyDataWrapper.daysInAp,
      differentUpperLimitThresholds = false
    )
  }

  def computeDouble(fyDataWrapper: DualFyDataWrapper[MarginalReliefConfig]): CalculatorResult = {
    import fyDataWrapper.{ fy1Values, fy2Values }

    val fy1Config: MarginalReliefConfig = fy1Values.fYConfig
    val fy2Config: MarginalReliefConfig = fy2Values.fYConfig

    val differentUpperLimitThresholds: Boolean = fy1Config.upperThreshold != fy2Config.upperThreshold

    val (associatedCompaniesFy1, associatedCompaniesFy2) = fyDataWrapper.associatedCompaniesAp.fold(
      (fy1Values.associatedCompaniesFY, fy2Values.associatedCompaniesFY) match {
        case (Some(associatedCompaniesFY1), None) => (associatedCompaniesFY1, associatedCompaniesFY1)
        case (None, Some(associatedCompaniesFY2)) => (associatedCompaniesFY2, associatedCompaniesFY2)
        case (Some(associatedCompaniesFY1), Some(associatedCompaniesFY2)) =>
          if (fy1Config.thresholdsMatch(otherConfig = fy2Config)) {
            val maxAssociatedCompanies: Int = Math.max(associatedCompaniesFY1, associatedCompaniesFY2)
            (maxAssociatedCompanies, maxAssociatedCompanies)
          } else {
            (associatedCompaniesFY1, associatedCompaniesFY2)
          }
        case (None, None) => (0, 0)
      }
    )(associatedCompanies => (associatedCompanies, associatedCompanies))

    val resultFy1: SingleResult[MarginalRate] = computeMarginalRateForFy(
      fyValues = fy1Values,
      associatedCompanies = associatedCompaniesFy1 + 1,
      daysInAp = fyDataWrapper.daysInAp,
      differentUpperLimitThresholds = differentUpperLimitThresholds
    )

    val resultFy2: SingleResult[MarginalRate] = computeMarginalRateForFy(
      fyValues = fy2Values,
      associatedCompanies = associatedCompaniesFy2 + 1,
      daysInAp = fyDataWrapper.daysInAp,
      differentUpperLimitThresholds = differentUpperLimitThresholds
    )

    resultFy1.combineWith(resultFy2)
  }
}
