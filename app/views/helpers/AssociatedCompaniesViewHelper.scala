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

import connectors.sharedmodel.{ AskAssociatedCompaniesParameter, AskBothParts, AskFull, AskOnePart, Period }
import forms.DateUtils.DateOps
import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukErrorMessage, GovukHint, GovukInput, GovukLabel }
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import viewmodels.govuk.all.FluentInput
import viewmodels.govuk.input.InputViewModel

object AssociatedCompaniesViewHelper extends ViewHelper {

  private val govukErrorMessage = new GovukErrorMessage
  private val govukHint = new GovukHint
  private val govukLabel = new GovukLabel
  private val govukInput = new GovukInput(govukErrorMessage, govukHint, govukLabel)

  def yesHtml(form: Form[_], a: AskAssociatedCompaniesParameter)(implicit messages: Messages): Option[Html] =
    a match {
      case AskFull | AskOnePart(_) =>
        Some(
          govukInput(
            InputViewModel(
              field = form("associatedCompaniesCount"),
              label = Label(content = Text(messages("associatedCompanies.countLabel")))
            )
              .withCssClass("govuk-!-width-one-third")
              .withAttribute("maxlength" -> "2")
          )
        )
      case _ =>
        None
    }

  def heading(a: AskAssociatedCompaniesParameter)(implicit messages: Messages): Html =
    h1(
      a match {
        case AskFull | AskBothParts(_, _) =>
          messages("associatedCompanies.heading")

        case AskOnePart(Period(start, end)) =>
          messages("associatedCompanies.heading.between", start.formatDateFull, end.formatDateFull)
      },
      classes = "govuk-heading-xl"
    )
}
