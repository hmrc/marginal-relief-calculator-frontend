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
import play.api.data.FormError

class AssociatedCompaniesFormProviderSpec extends OptionFieldBehaviours {

  val form = new AssociatedCompaniesFormProvider()()

  "form values" - {

    "Are valid" in {
      val range = integerBetween(0, 99)

      forAll(range) { range =>
        println(range)
        val data = buildDataMap("yes", Option(range.toString))
        val result = form.bind(data)
        result.hasErrors mustBe false
        result.value.value mustBe AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(range.toInt))
      }
    }
    "Are inValid" in {
      val data = buildDataMap("invalid value", Option("invalid value"))
      val result = form.bind(data)
      result.hasErrors mustBe true
      result.errors mustBe Seq(
        FormError("associatedCompanies", List("error.invalid"))
      )
    }

    "Optional value not sent valid" in {
      val data = buildDataMap("no")
      val result = form.bind(data)
      result.hasErrors mustBe false
    }

    "Optional value not sent invalid" in {
      val data = buildDataMap("yes")
      val result = form.bind(data)
      result.hasErrors mustBe true
      result.errors mustBe Seq(
        FormError("associatedCompaniesCount", List("associatedCompaniesCount.error.required"))
      )
    }

    "Associated Companies Count out of range above 99" in {

      val rangeAbove = intsAboveValue(99);

      forAll(rangeAbove) { rangeAbove =>
        val data = buildDataMap("yes", Option(rangeAbove.toString))
        val result = form.bind(data)
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("associatedCompaniesCount", List("associatedCompaniesCount.error.outOfRange"), Seq(0, 99))
        )
      }
    }

    "Associated Companies Count out of range below 0" in {

      val rangeBelow = intsBelowValue(0);

      forAll(rangeBelow) { rangeBelow =>
        val data = buildDataMap("yes", Option(rangeBelow.toString))
        val result = form.bind(data)
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("associatedCompaniesCount", List("associatedCompaniesCount.error.outOfRange"), Seq(0, 99))
        )
      }
    }
  }

  private def buildDataMap(associatedCompanies: String, associatedCompaniesCount: Option[String] = None) =
    Map(
      s"associatedCompanies" -> associatedCompanies
    ) ++ associatedCompaniesCount.map(c => "associatedCompaniesCount" -> c)
}
