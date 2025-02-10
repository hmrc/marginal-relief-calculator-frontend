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

import play.api.libs.json
import play.api.libs.json.{ JsError, JsObject, JsResult, JsString, JsValue, Json, OFormat }
import utils.RoundingUtils.roundUp

sealed trait TaxDetails {
  def year: Int
  def days: Int
  def corporationTax: BigDecimal
  def adjustedProfit: BigDecimal
  def adjustedDistributions: BigDecimal
  def taxRate: BigDecimal
  def adjustedAugmentedProfit: BigDecimal

  def roundValsUp: TaxDetails

  def fold[T](f: FlatRate => T)(g: MarginalRate => T): T =
    this match {
      case x: FlatRate     => f(x)
      case x: MarginalRate => g(x)
    }
}

object TaxDetails {
  implicit val flatRateFormat: OFormat[FlatRate] = Json.format[FlatRate]
  implicit val marginalRateFormat: OFormat[MarginalRate] = Json.format[MarginalRate]

  implicit val taxDetailsFormat: OFormat[TaxDetails] = new OFormat[TaxDetails] {
    def reads(json: JsValue): JsResult[TaxDetails] = (json \ "type").validate[String].flatMap {
      case "FlatRate"     => flatRateFormat.reads(json)
      case "MarginalRate" => marginalRateFormat.reads(json)
      case other          => JsError(s"Unknown type: $other")
    }

    def writes(td: TaxDetails): JsObject = td match {
      case fr: FlatRate     => flatRateFormat.writes(fr) + ("type"     -> JsString("FlatRate"))
      case mr: MarginalRate => marginalRateFormat.writes(mr) + ("type" -> JsString("MarginalRate"))
    }
  }
}

case class FlatRate(
  year: Int,
  corporationTax: BigDecimal,
  taxRate: BigDecimal,
  adjustedProfit: BigDecimal,
  adjustedDistributions: BigDecimal,
  adjustedAugmentedProfit: BigDecimal,
  days: Int
) extends TaxDetails {

  def roundValsUp: FlatRate = copy(
    corporationTax = roundUp(corporationTax),
    taxRate = roundUp(taxRate),
    adjustedProfit = roundUp(adjustedProfit),
    adjustedDistributions = roundUp(adjustedDistributions),
    adjustedAugmentedProfit = roundUp(adjustedAugmentedProfit)
  )
}

case class MarginalRate(
  year: Int,
  corporationTaxBeforeMR: BigDecimal,
  taxRateBeforeMR: BigDecimal,
  corporationTax: BigDecimal,
  taxRate: BigDecimal,
  marginalRelief: BigDecimal,
  adjustedProfit: BigDecimal,
  adjustedDistributions: BigDecimal,
  adjustedAugmentedProfit: BigDecimal,
  adjustedLowerThreshold: BigDecimal,
  adjustedUpperThreshold: BigDecimal,
  days: Int,
  fyRatio: FYRatio
) extends TaxDetails {

  def roundValsUp: MarginalRate = copy(
    corporationTaxBeforeMR = roundUp(corporationTaxBeforeMR),
    taxRateBeforeMR = roundUp(taxRateBeforeMR),
    corporationTax = roundUp(corporationTax),
    taxRate = roundUp(taxRate),
    marginalRelief = roundUp(marginalRelief),
    adjustedProfit = roundUp(adjustedProfit),
    adjustedDistributions = roundUp(adjustedDistributions),
    adjustedAugmentedProfit = roundUp(adjustedAugmentedProfit),
    adjustedLowerThreshold = roundUp(adjustedLowerThreshold),
    adjustedUpperThreshold = roundUp(adjustedUpperThreshold)
  )
}
