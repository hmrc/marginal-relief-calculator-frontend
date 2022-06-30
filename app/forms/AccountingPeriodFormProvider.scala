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
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AccountingPeriodFormProvider @Inject() extends Mappings {

  def apply(): Form[AccountingPeriodForm] =
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
      )(AccountingPeriodForm.apply)(AccountingPeriodForm.unapply)
    )
}
