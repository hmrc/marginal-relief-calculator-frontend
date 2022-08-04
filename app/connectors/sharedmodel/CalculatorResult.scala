/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.sharedmodel

import julienrf.json.derived
import play.api.libs.json.{ OFormat, __ }
import utils.CurrencyUtils.roundUp

sealed trait TaxDetails {
  def year: Int
  def days: Int
  def marginalRelief: Double
  def corporationTaxBeforeMR: Double
  def corporationTax: Double
}
case class FlatRate(year: Int, corporationTax: Double, taxRate: Double, adjustedProfit: Double, days: Int)
    extends TaxDetails {
  override def marginalRelief: Double = 0
  override def corporationTaxBeforeMR: Double = corporationTax
}
case class MarginalRate(
  year: Int,
  corporationTaxBeforeMR: Double,
  taxRateBeforeMR: Double,
  corporationTax: Double,
  taxRate: Double,
  marginalRelief: Double,
  adjustedProfit: Double,
  adjustedDistributions: Double,
  adjustedLowerThreshold: Double,
  adjustedUpperThreshold: Double,
  days: Int
) extends TaxDetails {
  def adjustedAugmentedProfit: Double = roundUp(BigDecimal(adjustedProfit) + BigDecimal(adjustedDistributions))
}

object TaxDetails {
  implicit val format: OFormat[TaxDetails] =
    derived.flat.oformat[TaxDetails]((__ \ "type").format[String])
}

sealed trait CalculatorResult {
  def totalDays: Int
  def totalMarginalRelief: Double
  def totalCorporationTaxBeforeMR: Double
  def totalCorporationTax: Double
}
object CalculatorResult {
  implicit val format: OFormat[CalculatorResult] =
    derived.flat.oformat[CalculatorResult]((__ \ "type").format[String])
}

case class SingleResult(
  details: TaxDetails
) extends CalculatorResult {
  override def totalDays: Int = details.days
  override def totalMarginalRelief: Double = details.marginalRelief
  override def totalCorporationTax: Double = details.corporationTax
  override def totalCorporationTaxBeforeMR: Double = details.corporationTaxBeforeMR
}

case class DualResult(
  year1: TaxDetails,
  year2: TaxDetails
) extends CalculatorResult {
  override def totalDays: Int = year1.days + year2.days
  override def totalMarginalRelief: Double = year1.marginalRelief + year2.marginalRelief
  override def totalCorporationTaxBeforeMR: Double = year1.corporationTaxBeforeMR + year2.corporationTaxBeforeMR
  override def totalCorporationTax: Double = year1.corporationTax + year2.corporationTax
}
