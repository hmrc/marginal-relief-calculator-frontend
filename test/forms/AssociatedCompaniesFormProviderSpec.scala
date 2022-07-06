package forms

import forms.behaviours.OptionFieldBehaviours
import models.AssociatedCompanies
import play.api.data.FormError

class AssociatedCompaniesFormProviderSpec extends OptionFieldBehaviours {

  val form = new AssociatedCompaniesFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "associatedCompanies.error.required"

    behave like optionsField[AssociatedCompanies](
      form,
      fieldName,
      validValues  = AssociatedCompanies.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
