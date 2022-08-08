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

import forms.behaviours.WholeAmountFieldBehaviours
import play.api.data.FormError
import utils.ConstraintsUtils.ONE_BILLION

class TaxableProfitFormProviderSpec extends WholeAmountFieldBehaviours {

  val form = new TaxableProfitFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = ONE_BILLION

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Map.empty,
      intsInRangeWithCommas(minimum, maximum)
    )

    behave like wholeAmountField(
      form,
      fieldName,
      Map.empty,
      nonNumericError = FormError(fieldName, "taxableProfit.error.nonNumeric"),
      doNotUseDecimalsError = FormError(fieldName, "taxableProfit.error.doNotUseDecimals"),
      outOfRangeError = FormError(fieldName, "error.outOfRange", List(Int.MinValue, Int.MaxValue))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "taxableProfit.error.required")
    )
  }
}
