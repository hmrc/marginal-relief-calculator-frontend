package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class DistributionsIncludedSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "DistributionsIncluded" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(DistributionsIncluded.values.toSeq)

      forAll(gen) {
        distributionsIncluded =>

          JsString(distributionsIncluded.toString).validate[DistributionsIncluded].asOpt.value mustEqual distributionsIncluded
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!DistributionsIncluded.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[DistributionsIncluded] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(DistributionsIncluded.values.toSeq)

      forAll(gen) {
        distributionsIncluded =>

          Json.toJson(distributionsIncluded) mustEqual JsString(distributionsIncluded.toString)
      }
    }
  }
}
