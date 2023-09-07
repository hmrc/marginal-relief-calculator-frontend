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

import calculator.computations.FlatRateComputation
import models.FlatRateConfig
import models.calculator._

object FlatRateCalculator extends FlatRateComputation {

  def computeSingle(fyDataWrapper: SingleFyDataWrapper[FlatRateConfig]): CalculatorResult = computeFlatRateForFy(
    fyValues = fyDataWrapper.fy1Values
  )

  def computeDouble(fyDataWrapper: DualFyDataWrapper[FlatRateConfig]): CalculatorResult = {
    val fy1Result: SingleResult[FlatRate] = computeFlatRateForFy(fyValues = fyDataWrapper.fy1Values)
    val fy2Result: SingleResult[FlatRate] = computeFlatRateForFy(fyValues = fyDataWrapper.fy2Values)

    fy1Result.combineWith(fy2Result)
  }
}
