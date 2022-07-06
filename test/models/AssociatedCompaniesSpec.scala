package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class AssociatedCompaniesSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "AssociatedCompanies" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(AssociatedCompanies.values.toSeq)

      forAll(gen) {
        associatedCompanies =>

          JsString(associatedCompanies.toString).validate[AssociatedCompanies].asOpt.value mustEqual associatedCompanies
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!AssociatedCompanies.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[AssociatedCompanies] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(AssociatedCompanies.values.toSeq)

      forAll(gen) {
        associatedCompanies =>

          Json.toJson(associatedCompanies) mustEqual JsString(associatedCompanies.toString)
      }
    }
  }
}
