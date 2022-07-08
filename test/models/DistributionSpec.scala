package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class DistributionSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "Distribution" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(Distribution.values.toSeq)

      forAll(gen) {
        distribution =>

          JsString(distribution.toString).validate[Distribution].asOpt.value mustEqual distribution
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!Distribution.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[Distribution] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(Distribution.values.toSeq)

      forAll(gen) {
        distribution =>

          Json.toJson(distribution) mustEqual JsString(distribution.toString)
      }
    }
  }
}
