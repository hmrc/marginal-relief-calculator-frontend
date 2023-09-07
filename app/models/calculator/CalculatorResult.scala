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

import play.api.libs.json.{ JsNumber, JsObject, JsString, Json, OWrites }
import utils.RoundingUtils.roundUp

sealed trait CalculatorResult {
  val effectiveTaxRate: BigDecimal
  val taxDetails: TaxDetails
  val `type`: String

  def roundValsUp: CalculatorResult
}

object CalculatorResult {
  implicit def writes[A <: CalculatorResult]: OWrites[A] = (o: A) => {
    val taxDetails = o match {
      case SingleResult(td: TaxDetails, _) => Map("details" -> Json.toJson(td))
      case DualResult(y1: TaxDetails, y2: TaxDetails, _) =>
        Map(
          "year1" -> Json.toJson(y1),
          "year2" -> Json.toJson(y2)
        )
    }

    JsObject(
      Map(
        "type"             -> JsString(o.`type`),
        "effectiveTaxRate" -> JsNumber(o.effectiveTaxRate)
      ) ++ taxDetails
    )
  }
}

case class SingleResult[I <: TaxDetails](taxDetails: I, effectiveTaxRate: BigDecimal) extends CalculatorResult {
  val `type`: String = "SingleResult"

  def roundValsUp: CalculatorResult = copy(
    taxDetails = taxDetails.roundValsUp,
    effectiveTaxRate = roundUp(effectiveTaxRate)
  )

  def combineWith[O <: TaxDetails](otherResult: SingleResult[O]): DualResult[I, O] = DualResult[I, O](
    year1TaxDetails = taxDetails,
    year2TaxDetails = otherResult.taxDetails,
    effectiveTaxRate = 100 * (
      (taxDetails.corporationTax + otherResult.taxDetails.corporationTax) /
        (taxDetails.adjustedProfit + otherResult.taxDetails.adjustedProfit)
    )
  )
}

case class DualResult[A <: TaxDetails, B <: TaxDetails](
  year1TaxDetails: A,
  year2TaxDetails: B,
  effectiveTaxRate: BigDecimal
) extends CalculatorResult {
  val `type`: String = "DualResult"
  val taxDetails: TaxDetails = year1TaxDetails

  def roundValsUp: CalculatorResult = copy(
    year1TaxDetails = year1TaxDetails.roundValsUp,
    year2TaxDetails = year2TaxDetails.roundValsUp,
    effectiveTaxRate = roundUp(effectiveTaxRate)
  )
}
