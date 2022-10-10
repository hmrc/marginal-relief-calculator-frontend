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
import utils.NumberUtils.roundUp

sealed trait TaxDetails {
  def year: Int
  def days: Int
  def corporationTax: Double
  def adjustedProfit: Double
  def adjustedDistributions: Double
  def taxRate: Double
  def adjustedAugmentedProfit: Double

  def fold[T](f: FlatRate => T)(g: MarginalRate => T): T =
    this match {
      case x: FlatRate     => f(x)
      case x: MarginalRate => g(x)
    }
}
case class FlatRate(
  year: Int,
  corporationTax: Double,
  taxRate: Double,
  adjustedProfit: Double,
  adjustedDistributions: Double,
  adjustedAugmentedProfit: Double,
  days: Int
) extends TaxDetails
case class MarginalRate(
  year: Int,
  corporationTaxBeforeMR: Double,
  taxRateBeforeMR: Double,
  corporationTax: Double,
  taxRate: Double,
  marginalRelief: Double,
  adjustedProfit: Double,
  adjustedDistributions: Double,
  adjustedAugmentedProfit: Double,
  adjustedLowerThreshold: Double,
  adjustedUpperThreshold: Double,
  days: Int
) extends TaxDetails

object TaxDetails {
  implicit val format: OFormat[TaxDetails] =
    derived.flat.oformat[TaxDetails]((__ \ "type").format[String])
}

sealed trait CalculatorResult {
  def totalDays: Int
  def totalMarginalRelief: Double
  def totalCorporationTaxBeforeMR: Double
  def totalCorporationTax: Double
  def effectiveTaxRate: Double
  def effectiveTaxRateBeforeMR: Double

  def fold[T](f: SingleResult => T)(g: DualResult => T): T =
    this match {
      case x: SingleResult => f(x)
      case x: DualResult   => g(x)
    }
}
object CalculatorResult {
  implicit val format: OFormat[CalculatorResult] =
    derived.flat.oformat[CalculatorResult]((__ \ "type").format[String])
}

case class SingleResult(
  details: TaxDetails,
  effectiveTaxRate: Double
) extends CalculatorResult {
  override def totalDays: Int = details.days
  override def totalMarginalRelief: Double = details.fold(_ => 0.0)(_.marginalRelief)
  override def totalCorporationTax: Double = details.corporationTax
  override def totalCorporationTaxBeforeMR: Double = details.fold(_.corporationTax)(_.corporationTaxBeforeMR)
  override def effectiveTaxRateBeforeMR: Double = (
    (BigDecimal(this.totalCorporationTaxBeforeMR) / BigDecimal(
      details.adjustedProfit
    )) * 100
  ).toDouble
}

case class DualResult(
  year1: TaxDetails,
  year2: TaxDetails,
  effectiveTaxRate: Double
) extends CalculatorResult {
  override def totalDays: Int = year1.days + year2.days
  override def totalMarginalRelief: Double = roundUp(
    BigDecimal(year1.fold(_ => 0.0)(_.marginalRelief)) + BigDecimal(year2.fold(_ => 0.0)(_.marginalRelief))
  )
  override def totalCorporationTaxBeforeMR: Double = roundUp(
    BigDecimal(year1.fold(_.corporationTax)(_.corporationTaxBeforeMR)) + BigDecimal(
      year2.fold(_.corporationTax)(_.corporationTaxBeforeMR)
    )
  )
  override def totalCorporationTax: Double = roundUp(
    BigDecimal(year1.corporationTax) + BigDecimal(year2.corporationTax)
  )

  override def effectiveTaxRateBeforeMR: Double =
    (((BigDecimal(year1.fold(_.corporationTax)(_.corporationTaxBeforeMR)) + BigDecimal(
      year2.fold(_.corporationTax)(_.corporationTaxBeforeMR)
    )) /
      (BigDecimal(year1.adjustedProfit) + BigDecimal(year2.adjustedProfit))) * 100).toDouble

}
