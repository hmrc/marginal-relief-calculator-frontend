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

import forms.behaviours.OptionFieldBehaviours
import models.AssociatedCompanies
import org.scalacheck.Shrink
import play.api.data.FormError

class AssociatedCompaniesFormProviderSpec extends OptionFieldBehaviours {

  implicit val noShrink: Shrink[BigInt] = Shrink.shrinkAny

  val form = new AssociatedCompaniesFormProvider()()

  "form values" - {

    "associatedCompanies" - {
      behave like optionsField[AssociatedCompanies](
        form,
        "associatedCompanies",
        Seq(AssociatedCompanies.Yes, AssociatedCompanies.No),
        FormError("associatedCompanies", "associatedCompanies.error.invalid")
      )
    }

    "associatedCompaniesCount" - {
      behave like associatedCompaniesCountBehaviours("associatedCompaniesCount")
    }

    "associatedCompaniesFY1Count" - {
      behave like associatedCompaniesCountBehaviours("associatedCompaniesFY1Count")
    }

    "associatedCompaniesFY2Count" - {
      behave like associatedCompaniesCountBehaviours("associatedCompaniesFY2Count")
    }

    def associatedCompaniesCountBehaviours(associatedCompaniesCountKey: String): Unit = {
      "bind valid values" in {
        forAll(integerBetween(1, 99) -> "validValues") { integer =>
          val result =
            form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> integer.toString))
          result.hasErrors mustBe false
          result.value.value mustBe (associatedCompaniesCountKey match {
            case "associatedCompaniesCount" =>
              AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(integer), None, None)
            case "associatedCompaniesFY1Count" =>
              AssociatedCompaniesForm(AssociatedCompanies.Yes, None, Some(integer), None)
            case "associatedCompaniesFY2Count" =>
              AssociatedCompaniesForm(AssociatedCompanies.Yes, None, None, Some(integer))
          })
        }
      }

      "bind to None when value empty" in {
        val result = form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> ""))
        result.hasErrors mustBe false
        result.value.value mustBe AssociatedCompaniesForm(AssociatedCompanies.Yes, None, None, None)
      }

      "return lessThanOne error when values are below 1" in {
        forAll(intsBelowValue(1) -> "belowMinValid") { integer =>
          val result =
            form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> integer.toString))
          result.hasErrors mustBe true
          result.errors mustBe Seq(
            FormError(associatedCompaniesCountKey, List("error.lessThanOne"), List(1))
          )
        }
      }

      "return greaterThan99 error when values are above 99" in {
        forAll(intsAboveValue(99) -> "belowMaxValid") { integer =>
          val result =
            form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> integer.toString))
          result.hasErrors mustBe true
          result.errors mustBe Seq(
            FormError(associatedCompaniesCountKey, List("error.greaterThan99"), List(99))
          )
        }
      }

      "return error when values decimals" in {
        forAll(decimals -> "decimals") { value =>
          val result = form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> value))
          result.hasErrors mustBe true
          result.errors mustBe Seq(
            FormError(associatedCompaniesCountKey, List("associatedCompaniesCount.error.wholeNumber"))
          )
        }
      }

      "return error when values are non-numbers" in {
        forAll(nonEmptyString -> "nonNumbers") { value =>
          val result = form.bind(buildDataMap(AssociatedCompanies.Yes, associatedCompaniesCountKey -> value))
          result.hasErrors mustBe true
          result.errors mustBe Seq(
            FormError(associatedCompaniesCountKey, List("associatedCompaniesCount.error.nonNumeric"))
          )
        }
      }
    }
  }

  private def buildDataMap(
    associatedCompanies: AssociatedCompanies,
    associatedCompaniesCount: (String, String)*
  ) =
    Map(
      s"associatedCompanies" -> associatedCompanies.toString
    ) ++ associatedCompaniesCount
}
