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

import forms.behaviours.PositiveWholeAmountFieldBehaviours
import play.api.data.FormError

class TaxableProfitFormProviderSpec extends PositiveWholeAmountFieldBehaviours {

  val form = new TaxableProfitFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = 1000000000

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Map.empty,
      intsInRangeWithCommas(minimum, maximum)
    )

    behave like positiveWholeAmountField(
      form,
      fieldName,
      Map.empty,
      nonNumericError = FormError(fieldName, "taxableProfit.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "taxableProfit.error.wholeNumber"),
      doNotUseDecimalsError = FormError(fieldName, "taxableProfit.error.doNotUseDecimals"),
      outOfRangeError = FormError(fieldName, "taxableProfit.error.outOfRange", List(1, 1000000000))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "taxableProfit.error.required")
    )
  }
}
