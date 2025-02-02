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

import utils.RoundingUtils.roundUp
import play.api.libs.json.*
import TaxDetails.taxDetailsFormat

sealed trait CalculatorResult {
  val effectiveTaxRate: BigDecimal
  val taxDetails: TaxDetails
  val `type`: String

  def totalMarginalRelief: Double
  def totalCorporationTax: Double
  def totalCorporationTaxBeforeMR: Double
  def effectiveTaxRateBeforeMR: Double

  def roundValsUp: CalculatorResult

    def fold[T](f: SingleResult[TaxDetails] => T)(g: DualResult[TaxDetails, TaxDetails] => T): T =
      this match {
        case x: SingleResult[_] => f(x.asInstanceOf[SingleResult[TaxDetails]])
        case x: DualResult[_, _] => g(x.asInstanceOf[DualResult[TaxDetails, TaxDetails]])
      }
}

object CalculatorResult {
  implicit def writes[A <: CalculatorResult]: OWrites[A] = new OWrites[A] {
    def writes(o: A): JsObject = {
      val taxDetails = o match {
          case s: SingleResult[_] => Json.obj("details" -> Json.toJson(s.taxDetails)(taxDetailsFormat))
          case d: DualResult[_, _] =>
            Json.obj(
              "year1" -> Json.toJson(d.year1TaxDetails)(taxDetailsFormat),
              "year2" -> Json.toJson(d.year2TaxDetails)(taxDetailsFormat)
            )

      }
      Json.obj(
        "type" -> JsString(o.`type`),
        "effectiveTaxRate" -> JsNumber(o.effectiveTaxRate)
      ) ++ taxDetails
    }
  }
}

case class SingleResult[I <: TaxDetails](taxDetails: I, effectiveTaxRate: BigDecimal) extends CalculatorResult {
  val `type`: String = "SingleResult"

  override def totalMarginalRelief: Double = taxDetails.fold(_ => 0.0)(_.marginalRelief.doubleValue)

  override def totalCorporationTax: Double = taxDetails.corporationTax.doubleValue

  override def totalCorporationTaxBeforeMR: Double =
    taxDetails.fold(_.corporationTax)(_.corporationTaxBeforeMR).doubleValue

  override def effectiveTaxRateBeforeMR: Double =
    ((this.totalCorporationTaxBeforeMR / taxDetails.adjustedProfit) * 100).toDouble

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
  def totalDays: Int = year1TaxDetails.days + year2TaxDetails.days
  override def totalCorporationTax: Double =
    (year1TaxDetails.fold(_.corporationTax)(_.corporationTax) +
      year2TaxDetails.fold(_.corporationTax)(_.corporationTax)).doubleValue

  override def totalCorporationTaxBeforeMR: Double =
    (year1TaxDetails.fold(_.corporationTax)(_.corporationTaxBeforeMR) +
      year2TaxDetails.fold(_.corporationTax)(_.corporationTaxBeforeMR)).doubleValue

  override def effectiveTaxRateBeforeMR: Double =
    ((year1TaxDetails.fold(_.corporationTax)(_.corporationTaxBeforeMR) +
      year2TaxDetails.fold(_.corporationTax)(_.corporationTaxBeforeMR)) /
      (year1TaxDetails.adjustedProfit + year2TaxDetails.adjustedProfit) * 100).doubleValue

  override def totalMarginalRelief: Double =
    year1TaxDetails.fold(_ => 0.0)(_.marginalRelief.doubleValue) +
      year2TaxDetails.fold(_ => 0.0)(_.marginalRelief.doubleValue)
}
