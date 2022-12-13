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

package forms

import forms.behaviours.IntFieldBehaviours
import org.scalacheck.Shrink
import play.api.data.FormError

class TwoAssociatedCompaniesFormProviderSpec extends IntFieldBehaviours {

  implicit val noShrink: Shrink[Int] = Shrink.shrinkAny

  val form = new TwoAssociatedCompaniesFormProvider()(1, 2)

  "TwoAssociatedCompaniesFormProvider" - {

    val minimum = 1
    val maximum = 99

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    "not bind valid values when not provided" in {
      val result =
        form.bind(
          Map.empty[String, String]
        )
      result.hasErrors mustBe false
      result.value.value mustBe TwoAssociatedCompaniesForm(None, None)
    }

    "bind valid values when valid" in {
      forAll(validDataGenerator -> "validValues") { integer =>
        val result =
          form.bind(
            Map("associatedCompaniesFY1Count" -> integer, "associatedCompaniesFY2Count" -> integer)
          )
        result.hasErrors mustBe false
        result.value.value mustBe TwoAssociatedCompaniesForm(Some(integer.toInt), Some(integer.toInt))
      }
    }

    "return lessThanOne error when values are below 1" in {
      forAll(intsBelowValue(0) -> "belowMinValid") { integer =>
        val result =
          form.bind(
            Map("associatedCompaniesFY1Count" -> integer.toString, "associatedCompaniesFY2Count" -> integer.toString)
          )

        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError(
            "associatedCompaniesFY1Count",
            List("twoAssociatedCompanies.error.lessThanZero"),
            List(0, "1", "2")
          ),
          FormError("associatedCompaniesFY2Count", List("twoAssociatedCompanies.error.lessThanZero"), List(0, "2", "3"))
        )
      }
    }

    "return greaterThan99 error when values are above 99" in {
      forAll(intsAboveValue(100) -> "aboveMaxValid") { integer =>
        val result =
          form.bind(
            Map("associatedCompaniesFY1Count" -> integer.toString, "associatedCompaniesFY2Count" -> integer.toString)
          )
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError(
            "associatedCompaniesFY1Count",
            List("twoAssociatedCompanies.error.greaterThan99"),
            List(99, "1", "2")
          ),
          FormError(
            "associatedCompaniesFY2Count",
            List("twoAssociatedCompanies.error.greaterThan99"),
            List(99, "2", "3")
          )
        )
      }
    }

    "return error when values are decimals" in {
      forAll(decimals -> "decimals") { value =>
        val result =
          form.bind(Map("associatedCompaniesFY1Count" -> value, "associatedCompaniesFY2Count" -> value))
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("associatedCompaniesFY1Count", List("twoAssociatedCompanies.error.wholeNumber")),
          FormError("associatedCompaniesFY2Count", List("twoAssociatedCompanies.error.wholeNumber"))
        )
      }
    }

    "return error when values are non-numbers" in {
      forAll(nonEmptyString -> "nonNumbers") { value =>
        val result =
          form.bind(Map("associatedCompaniesFY1Count" -> value, "associatedCompaniesFY2Count" -> value))
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("associatedCompaniesFY1Count", List("twoAssociatedCompanies.error.nonNumeric")),
          FormError("associatedCompaniesFY2Count", List("twoAssociatedCompanies.error.nonNumeric"))
        )
      }
    }
  }
}
