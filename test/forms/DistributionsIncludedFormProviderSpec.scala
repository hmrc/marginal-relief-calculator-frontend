package forms

import forms.behaviours.OptionFieldBehaviours
import models.DistributionsIncluded
import play.api.data.FormError

class DistributionsIncludedFormProviderSpec extends OptionFieldBehaviours {

  val form = new DistributionsIncludedFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "distributionsIncluded.error.required"

    behave like optionsField[DistributionsIncluded](
      form,
      fieldName,
      validValues  = DistributionsIncluded.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
