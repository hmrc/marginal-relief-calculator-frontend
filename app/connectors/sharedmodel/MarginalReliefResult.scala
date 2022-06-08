package connectors.sharedmodel

import julienrf.json.derived
import play.api.libs.json.{Format, Json, OFormat, __}

case class MarginalReliefByYear(
                                 year: Int,
                                 corporationTaxBeforeMR: Double,
                                 effectiveTaxRateBeforeMR: Double,
                                 corporationTax: Double,
                                 effectiveTaxRate: Double,
                                 marginalRelief: Double
                               )
object MarginalReliefByYear {
  implicit val format: Format[MarginalReliefByYear] = Json.format[MarginalReliefByYear]
}

sealed trait MarginalReliefResult
object MarginalReliefResult {
  implicit val format: OFormat[MarginalReliefResult] =
    derived.flat.oformat[MarginalReliefResult]((__ \ "type").format[String])
}

case class SingleResult(
                         corporationTaxBeforeMR: Double,
                         effectiveTaxRateBeforeMR: Double,
                         corporationTax: Double,
                         effectiveTaxRate: Double,
                         marginalRelief: Double
                       ) extends MarginalReliefResult
case class DualResult(
                       year1: MarginalReliefByYear,
                       year2: MarginalReliefByYear,
                       effectiveTaxRateBeforeMR: Double,
                       effectiveTaxRate: Double
                     ) extends MarginalReliefResult