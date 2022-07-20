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
import models.DistributionsIncluded
import play.api.data.FormError

class DistributionsIncludedFormProviderSpec extends OptionFieldBehaviours {

  val form = new DistributionsIncludedFormProvider()()

  "form values" - {

    "distributionsIncluded" - {
      behave like optionsField[DistributionsIncluded](
        form,
        "distributionsIncluded",
        Seq(DistributionsIncluded.Yes, DistributionsIncluded.No),
        FormError("distributionsIncluded", "distributionsIncluded.error.invalid")
      )
    }

    def distributionsIncludedAmountBehaviours(distributionsIncludedCountKey: String): Unit = {
      "bind valid values" in {
        forAll(integerBetween(0, 99) -> "validValues") { integer =>
          val result =
            form.bind(buildDataMap(DistributionsIncluded.Yes, distributionsIncludedCountKey -> integer.toString))
          result.hasErrors mustBe false
          result.value.value mustBe DistributionsIncludedForm(DistributionsIncluded.Yes, Some(integer))
        }
      }

  }

  private def buildDataMap(
                            distributionsIncluded: DistributionsIncluded,
                            distributionsIncludedAmount: (String, String)*
                          ) =
    Map(
      s"distributionsIncluded" -> distributionsIncluded.toString
    ) ++ distributionsIncludedAmount
}


