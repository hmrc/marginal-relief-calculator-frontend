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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import models.DistributionsIncluded
import play.api.data.Forms.mapping
import utils.ConstraintsUtils.ONE_BILLION

class DistributionsIncludedFormProvider @Inject() extends Mappings {
  def apply(): Form[DistributionsIncludedForm] =
    Form {
      mapping(
        "distributionsIncluded" -> enumerable[DistributionsIncluded](
          "distributionsIncluded.error.required",
          "distributionsIncluded.error.invalid"
        ),
        "distributionsIncludedAmount" -> conditional[Int, DistributionsIncluded](
          wholeAmount(
            "distributionsIncludedAmount.error.required",
            "error.outOfRange",
            "distributionsIncludedAmount.error.doNotUseDecimals",
            "distributionsIncludedAmount.error.nonNumeric",
            KeyRange("distributionsIncludedAmount.error.lessThanOne", "error.greaterThanOneBillion"),
            ValueRange(1, ONE_BILLION)
          ).withPrefix("distributionsIncludedAmount"),
          enumerable[DistributionsIncluded]().withPrefix("distributionsIncluded"),
          _ == DistributionsIncluded.Yes
        )
      )(DistributionsIncludedForm.apply)(o => Some(Tuple.fromProductTyped(o)))
    }
}
