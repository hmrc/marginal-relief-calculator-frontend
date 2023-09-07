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

import models.FlatRateConfig
import models.calculator.{ FlatRate, FyValues, SingleResult }

trait FlatRateComputation {

  def computeFlatRateForFy(fyValues: FyValues[FlatRateConfig]): SingleResult[FlatRate] = {
    import fyValues.{ adjustedDistributions, adjustedProfit, apDaysInFY, fy }
    import fyValues.fYConfig.mainRate

    val taxRate: BigDecimal = mainRate * 100

    SingleResult[FlatRate](
      taxDetails = FlatRate(
        year = fy,
        corporationTax = BigDecimal(mainRate) * adjustedProfit,
        taxRate = taxRate,
        adjustedProfit = adjustedProfit,
        adjustedDistributions = adjustedDistributions,
        adjustedAugmentedProfit = adjustedProfit + adjustedDistributions,
        days = apDaysInFY
      ),
      effectiveTaxRate = taxRate
    )
  }
}
