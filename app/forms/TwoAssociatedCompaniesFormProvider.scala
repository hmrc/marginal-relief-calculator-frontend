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

  def apply(year1: Int, year2: Int): Form[TwoAssociatedCompaniesForm] =
    Form(
      mapping(
        "associatedCompaniesFY1Count" ->
          optional(
            int(
              "twoAssociatedCompanies.error.required",
              "twoAssociatedCompanies.error.wholeNumber",
              "twoAssociatedCompanies.error.nonNumeric"
            ).verifying(
              minimumValueWithDynamicMessage(
                0,
                "twoAssociatedCompanies.error.lessThanZero",
                0,
                year1.toString,
                (year1 + 1).toString
              ),
              maximumValueWithDynamicMessage(
                99,
                "twoAssociatedCompanies.error.greaterThan99",
                99,
                year1.toString,
                (year1 + 1).toString
              )
            )
          ),
        "associatedCompaniesFY2Count" ->
          optional(
            int(
              "twoAssociatedCompanies.error.required",
              "twoAssociatedCompanies.error.wholeNumber",
              "twoAssociatedCompanies.error.nonNumeric"
            ).verifying(
              minimumValueWithDynamicMessage(
                0,
                "twoAssociatedCompanies.error.lessThanZero",
                0,
                year2.toString,
                (year2 + 1).toString
              ),
              maximumValueWithDynamicMessage(
                99,
                "twoAssociatedCompanies.error.greaterThan99",
                99,
                year2.toString,
                (year2 + 1).toString
              )
            )
          )
      )(TwoAssociatedCompaniesForm.apply)(TwoAssociatedCompaniesForm.unapply)
    )
}
