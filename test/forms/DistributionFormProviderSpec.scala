package forms

import forms.behaviours.OptionFieldBehaviours
import models.Distribution
import play.api.data.FormError

class DistributionFormProviderSpec extends OptionFieldBehaviours {

  val form = new DistributionFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "distribution.error.required"

    behave like optionsField[Distribution](
      form,
      fieldName,
      validValues  = Distribution.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
