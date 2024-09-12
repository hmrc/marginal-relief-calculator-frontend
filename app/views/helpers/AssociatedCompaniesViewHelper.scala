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

package views.helpers

import models.associatedCompanies.{ AskBothParts, AskFull, AskOnePart, AssociatedCompaniesParameter, Period }
import forms.DateUtils.DateOps
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukErrorMessage, GovukHint, GovukInput, GovukLabel }
import uk.gov.hmrc.govukfrontend.views.html.helpers.{ GovukFormGroup, GovukHintAndErrorMessage }
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import viewmodels.govuk.All.FluentInput
import viewmodels.govuk.input.InputViewModel

object AssociatedCompaniesViewHelper extends ViewHelper {

  private val govukErrorMessage = new GovukErrorMessage
  private val govukHint = new GovukHint
  private val govukHintAndErrorMessage = new GovukHintAndErrorMessage(govukHint, govukErrorMessage)
  private val govukFormGroup = new GovukFormGroup
  private val govukLabel = new GovukLabel
  private val govukInput = new GovukInput(govukLabel, govukFormGroup, govukHintAndErrorMessage)

  def yesHtml(form: Form[_], a: AssociatedCompaniesParameter)(implicit messages: Messages): Option[Html] =
    a match {
      case AskFull | AskOnePart(_) =>
        Some(
          govukInput(
            InputViewModel(
              field = form("associatedCompaniesCount"),
              label = Label(content = Text(messages("associatedCompanies.countLabel")))
            )
              .withCssClass("govuk-input--width-2")
          )
        )
      case _ =>
        None
    }

  def heading(a: AssociatedCompaniesParameter)(implicit messages: Messages): String =
    a match {
      case AskFull | AskBothParts(_, _) =>
        messages("associatedCompanies.heading")
      case AskOnePart(Period(start, end)) =>
        messages("associatedCompanies.heading.between", start.govDisplayFormat, end.govDisplayFormat)
      case _ => ""
    }
}
