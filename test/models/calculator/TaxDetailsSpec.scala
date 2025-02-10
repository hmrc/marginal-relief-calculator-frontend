package models.calculator

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*

import scala.language.postfixOps
import scala.math.BigDecimal.RoundingMode

class TaxDetailsSpec extends AnyFreeSpec with Matchers {

  val flatRateExample = FlatRate(
    year = 2023,
    corporationTax = BigDecimal(10000.567),
    taxRate = BigDecimal(0.19),
    adjustedProfit = BigDecimal(50000.123),
    adjustedDistributions = BigDecimal(2000.789),
    adjustedAugmentedProfit = BigDecimal(52000.456),
    days = 365
  )

  val marginalRateExample = MarginalRate(
    year = 2023,
    corporationTaxBeforeMR = BigDecimal(12000.456),
    taxRateBeforeMR = BigDecimal(0.25),
    corporationTax = BigDecimal(11000.789),
    taxRate = BigDecimal(0.21),
    marginalRelief = BigDecimal(500.123),
    adjustedProfit = BigDecimal(60000.234),
    adjustedDistributions = BigDecimal(2500.567),
    adjustedAugmentedProfit = BigDecimal(62500.678),
    adjustedLowerThreshold = BigDecimal(50000),
    adjustedUpperThreshold = BigDecimal(250000),
    days = 365,
    fyRatio = FYRatio(BigDecimal(1.0), 1)
  )

  def roundUp(value: BigDecimal): BigDecimal = value.setScale(0, RoundingMode.UP)

  "TaxDetails" - {

    "Serialization (writes)" - {

      "should serialize FlatRate correctly" in {
        val json = TaxDetails.taxDetailsFormat.writes(flatRateExample)

        (json \ "type").as[String] shouldBe "FlatRate"
        (json \ "year").as[Int] shouldBe 2023
        (json \ "corporationTax").as[BigDecimal] shouldBe BigDecimal(10000.567)
      }

      "should serialize MarginalRate correctly" in {
        val json = TaxDetails.taxDetailsFormat.writes(marginalRateExample)

        (json \ "type").as[String] shouldBe "MarginalRate"
        (json \ "year").as[Int] shouldBe 2023
        (json \ "corporationTax").as[BigDecimal] shouldBe BigDecimal(11000.789)
      }
    }

    "Deserialization (reads)" - {

      "should deserialize FlatRate correctly" in {
        val json = Json.parse(
          s"""{
             |  "type": "FlatRate",
             |  "year": 2023,
             |  "corporationTax": 10000.567,
             |  "taxRate": 0.19,
             |  "adjustedProfit": 50000.123,
             |  "adjustedDistributions": 2000.789,
             |  "adjustedAugmentedProfit": 52000.456,
             |  "days": 365
             |}""".stripMargin
        )

        TaxDetails.taxDetailsFormat.reads(json) match {
          case JsSuccess(value, _) =>
            value shouldBe flatRateExample
          case JsError(errors) =>
            fail(s"Failed to deserialize FlatRate JSON. Errors: $errors")
        }
      }

      "should deserialize MarginalRate correctly" in {
        val json = Json.parse(
          s"""{
             |  "type": "MarginalRate",
             |  "year": 2023,
             |  "corporationTaxBeforeMR": 12000.456,
             |  "taxRateBeforeMR": 0.25,
             |  "corporationTax": 11000.789,
             |  "taxRate": 0.21,
             |  "marginalRelief": 500.123,
             |  "adjustedProfit": 60000.234,
             |  "adjustedDistributions": 2500.567,
             |  "adjustedAugmentedProfit": 62500.678,
             |  "adjustedLowerThreshold": 50000,
             |  "adjustedUpperThreshold": 250000,
             |  "days": 365,
             |  "fyRatio": { "numerator": 1.0, "denominator": 1.0 }
             |}""".stripMargin
        )

        TaxDetails.taxDetailsFormat.reads(json) match {
          case JsSuccess(value, _) =>
            value shouldBe marginalRateExample
          case JsError(errors) =>
            fail(s"Failed to deserialize MarginalRate JSON. Errors: $errors")
        }
      }

      "should return error for unknown type" in {
        val json = Json.parse("""{ "type": "UnknownType" }""")
        TaxDetails.taxDetailsFormat.reads(json) shouldBe a[JsError]
      }

      "should return error for missing required fields" in {
        val json = Json.parse("""{ "type": "FlatRate", "year": 2023 }""")
        TaxDetails.taxDetailsFormat.reads(json) shouldBe a[JsError]
      }
    }

    "Business Logic" - {

      "should correctly round up values for FlatRate" in {
        val rounded = flatRateExample.roundValsUp

        rounded shouldBe flatRateExample.copy(
          corporationTax = BigDecimal(10000.57),
          taxRate = BigDecimal(0.19),
          adjustedProfit = BigDecimal(50000.12),
          adjustedDistributions = BigDecimal(2000.79),
          adjustedAugmentedProfit = BigDecimal(52000.46)
        )
      }

      "should correctly round up values for MarginalRate" in {
        val rounded = marginalRateExample.roundValsUp

        rounded shouldBe marginalRateExample.copy(
          corporationTaxBeforeMR = BigDecimal(12000.46),
          taxRateBeforeMR = BigDecimal(0.25),
          corporationTax = BigDecimal(11000.79),
          taxRate = BigDecimal(0.21),
          marginalRelief = BigDecimal(500.12),
          adjustedProfit = BigDecimal(60000.23),
          adjustedDistributions = BigDecimal(2500.57),
          adjustedAugmentedProfit = BigDecimal(62500.68),
          adjustedLowerThreshold = BigDecimal(50000.00),
          adjustedUpperThreshold = BigDecimal(250000.00),
          days = 365,
          fyRatio = FYRatio(BigDecimal(1.0), 1)
        )
      }
    }
  }
}
