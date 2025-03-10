/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.mappings.{ EndLocalDateFormatter, LocalDateFormatter, Mappings }
import play.api.data.Form
import play.api.data.Forms.{ mapping, of, optional }
import play.api.i18n.Messages

class AccountingPeriodFormProvider extends Mappings {

  def apply(implicit messages: Messages): Form[AccountingPeriodForm] =
    Form(
      mapping(
        "accountingPeriodStartDate" -> of(
          new LocalDateFormatter(
            invalidKey = "accountingPeriodStartDate.error.invalid",
            allRequiredKey = "accountingPeriodStartDate.error.required.all",
            twoRequiredKey = "accountingPeriodStartDate.error.required.two",
            requiredKey = "accountingPeriodStartDate.error.required"
          )
        ),
        "accountingPeriodEndDate" -> optional(
          of(
            new EndLocalDateFormatter(
              invalidKey = "accountingPeriodEndDate.error.invalid",
              allRequiredKey = "accountingPeriodEndDate.error.required.all",
              twoRequiredKey = "accountingPeriodEndDate.error.required.two",
              startDateId = "accountingPeriodStartDate",
              requiredKey = "accountingPeriodEndDate.error.required"
            )
          )
        )
      )(AccountingPeriodForm.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
}
