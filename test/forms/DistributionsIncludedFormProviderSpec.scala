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

import forms.behaviours.{ OptionFieldBehaviours, WholeAmountFieldBehaviours }
import models.DistributionsIncluded
import org.scalacheck.Shrink
import play.api.data.FormError
import utils.ConstraintsUtils.ONE_BILLION

class DistributionsIncludedFormProviderSpec extends OptionFieldBehaviours with WholeAmountFieldBehaviours {
  implicit val noShrinkLong: Shrink[Long] = Shrink.shrinkAny

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

    "distributions Included Amount" - {
      val fieldName = "distributionsIncludedAmount"

      val minimum = 1
      val maximum = ONE_BILLION

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        Map("distributionsIncluded" -> DistributionsIncluded.Yes.toString),
        intsInRangeWithCommas(minimum, maximum)
      )

      behave like wholeAmountField(
        form,
        fieldName,
        Map("distributionsIncluded" -> DistributionsIncluded.Yes.toString),
        nonNumericError = FormError(fieldName, "distributionsIncludedAmount.error.nonNumeric"),
        doNotUseDecimalsError = FormError(fieldName, "distributionsIncludedAmount.error.doNotUseDecimals"),
        outOfRangeError = FormError(fieldName, "error.outOfRange", List(1, ONE_BILLION))
      )

      "bind to None when value empty" in {
        val result = form.bind(buildDataMap(DistributionsIncluded.Yes))
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("distributionsIncludedAmount", "distributionsIncludedAmount.error.required")
        )
      }

      "return greater than billion error" in {
        val result = form.bind(
          buildDataMap(DistributionsIncluded.Yes, "distributionsIncludedAmount" -> (ONE_BILLION.toLong + 1).toString)
        )
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("distributionsIncludedAmount", "error.greaterThanOneBillion")
        )
      }

      "return less than one error" in {
        val result = form.bind(buildDataMap(DistributionsIncluded.Yes, "distributionsIncludedAmount" -> 0.toString))
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("distributionsIncludedAmount", "error.lessThanOne")
        )
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
