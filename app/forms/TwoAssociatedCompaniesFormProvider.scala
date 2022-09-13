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

class TwoAssociatedCompaniesFormProvider @Inject() extends Mappings {

  def apply(): Form[TwoAssociatedCompaniesForm] =
    Form(
      mapping(
        "associatedCompaniesFY1Count" ->
          optional(
            int(
              "associatedCompaniesCount.error.required",
              "associatedCompaniesCount.error.wholeNumber",
              "associatedCompaniesCount.error.nonNumeric"
            ).verifying(minimumValue(0, "error.lessThanZero"), maximumValue(99, "error.greaterThan99"))
          ),
        "associatedCompaniesFY2Count" ->
          optional(
            int(
              "associatedCompaniesCount.error.required",
              "associatedCompaniesCount.error.wholeNumber",
              "associatedCompaniesCount.error.nonNumeric"
            ).verifying(minimumValue(0, "error.lessThanZero"), maximumValue(99, "error.greaterThan99"))
          )
      )(TwoAssociatedCompaniesForm.apply)(TwoAssociatedCompaniesForm.unapply)
    )
}
