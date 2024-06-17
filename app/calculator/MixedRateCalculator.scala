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

import calculator.computations.{ FlatRateComputation, MarginalRateComputation }
import models.calculator._

object MixedRateCalculator extends FlatRateComputation with MarginalRateComputation {
  def compute(fyDataWrapper: MixedFyDataWrapper): CalculatorResult = {
    import fyDataWrapper.{ fyFlatValues, fyMarginalValues }

    val flatResult: SingleResult[FlatRate] = computeFlatRateForFy(fyFlatValues)

    val associatedCompanies: Int = fyDataWrapper.associatedCompaniesAp.getOrElse(
      fyMarginalValues.associatedCompaniesFY.getOrElse(0)
    ) + 1

    val marginalResult: SingleResult[MarginalRate] = computeMarginalRateForFy(
      fyValues = fyMarginalValues,
      associatedCompanies = associatedCompanies,
      daysInAp = fyDataWrapper.daysInAp,
      differentUpperLimitThresholds = true
    )

    if (fyFlatValues.fy < fyMarginalValues.fy) { flatResult.combineWith(marginalResult) }
    else {
      marginalResult.combineWith(flatResult)
    }
  }
}
