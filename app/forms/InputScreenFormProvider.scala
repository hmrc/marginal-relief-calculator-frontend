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

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.{ mapping, optional }

import javax.inject.Inject

class InputScreenFormProvider @Inject() extends Mappings {

  def apply(): Form[InputScreenForm] =
    Form(
      mapping(
        "accountingPeriodStartDate" -> localDate(
          invalidKey = "accountingPeriodStartDate.error.invalid",
          allRequiredKey = "accountingPeriodStartDate.error.required.all",
          twoRequiredKey = "accountingPeriodStartDate.error.required.two",
          requiredKey = "accountingPeriodStartDate.error.required"
        ),
        "accountingPeriodEndDate" -> optional(
          localDate(
            invalidKey = "accountingPeriodEndDate.error.invalid",
            allRequiredKey = "accountingPeriodEndDate.error.required.all",
            twoRequiredKey = "accountingPeriodEndDate.error.required.two",
            requiredKey = "accountingPeriodEndDate.error.required"
          )
        ),
        "profit" -> int("profit.error.required", "profit.error.wholeNumber", "profit.error.nonNumeric")
          .verifying(inRange(0, Int.MaxValue, "profit.error.outOfRange")),
        "distribution" -> int(
          "distribution.error.required",
          "distribution.error.wholeNumber",
          "distribution.error.nonNumeric"
        )
          .verifying(inRange(0, Int.MaxValue, "distribution.error.outOfRange")),
        "associatedCompanies" -> int(
          "associatedCompanies.error.required",
          "associatedCompanies.error.wholeNumber",
          "associatedCompanies.error.nonNumeric"
        )
          .verifying(inRange(0, Int.MaxValue, "associatedCompanies.error.outOfRange"))
      )(InputScreenForm.apply)(InputScreenForm.unapply)
    )
}
