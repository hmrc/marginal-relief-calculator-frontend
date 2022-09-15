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
import models.AssociatedCompanies
import play.api.data.Form
import play.api.data.Forms.{ mapping, optional }
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import javax.inject.Inject

class AssociatedCompaniesFormProvider @Inject() extends Mappings {

  def apply(): Form[AssociatedCompaniesForm] =
    Form {
      mapping(
        "associatedCompanies" -> enumerable[AssociatedCompanies](
          "associatedCompanies.error.required",
          "associatedCompanies.error.invalid"
        ),
        "associatedCompaniesCount" -> mandatoryIfEqual(
          "associatedCompanies",
          "yes",
          optional(
            int(
              "associatedCompaniesCount.error.required",
              "associatedCompaniesCount.error.wholeNumber",
              "associatedCompaniesCount.error.nonNumeric"
            ).verifying(minimumValue(1, "error.lessThanOne"), maximumValue(99, "error.greaterThan99"))
          )
        )
      )((v1, v2) => AssociatedCompaniesForm(v1, v2.flatten))(form =>
        Some((form.associatedCompanies, Some(form.associatedCompaniesCount)))
      )
    }
}
