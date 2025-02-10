package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import utils.DecimalToFractionUtils.{ Fraction, toFraction }

class DecimalToFractionUtilsSpec extends AnyWordSpec with Matchers {

  "DecimalToFractionUtils.toFraction" should {

    "convert whole numbers correctly" in {
      toFraction(5.0) shouldBe Fraction(5, 1)
      toFraction(10.0) shouldBe Fraction(10, 1)
      toFraction(-3.0) shouldBe Fraction(-3, 1)
    }

    "convert simple decimals correctly" in {
      toFraction(0.5) shouldBe Fraction(1, 2)
      toFraction(0.25) shouldBe Fraction(1, 4)
      toFraction(0.75) shouldBe Fraction(3, 4)
      toFraction(1.5) shouldBe Fraction(3, 2)
    }

    "handle numbers with many decimal places" in {
      toFraction(0.125) shouldBe Fraction(1, 8)
      toFraction(0.0625) shouldBe Fraction(1, 16)
      toFraction(0.2) shouldBe Fraction(1, 5)
    }

    "handle zero correctly" in {
      toFraction(0.0) shouldBe Fraction(0, 1)
    }

    "serialize and deserialize Fraction JSON correctly" in {
      val fraction = Fraction(3, 4)
      val json = Json.toJson(fraction)
      json.toString() shouldBe """{"numerator":3,"denominator":4}"""

      val parsedFraction = json.as[Fraction]
      parsedFraction shouldBe fraction
    }

  }
}
