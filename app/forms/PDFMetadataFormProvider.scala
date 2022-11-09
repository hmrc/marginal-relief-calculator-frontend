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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, optional}

class PDFMetadataFormProvider @Inject() extends Mappings {

  def apply(): Form[PDFMetadataForm] =
    Form(
      mapping(
        "companyName" -> optional(
          text()
            .verifying(maxLength(160, "pDFMetadata.companyname.error.length"))
        ),
        "utr" -> optional(
          utrMapper(
            "taxableProfit.error.required",
            "Do not use decimal points. Enter a whole number. For example, 1.",
            "Enter a valid UTR number. For example, 1234567891"
          )
//          text()
//            .verifying(maxLength(15, "pDFMetadata.utr.error.length"))
        )
      )(PDFMetadataForm.apply)(PDFMetadataForm.unapply)
    )
}
