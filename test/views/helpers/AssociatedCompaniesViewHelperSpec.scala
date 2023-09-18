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

import models.associatedCompanies.{ AskBothParts, AskFull, AskOnePart, Period }
import forms.AssociatedCompaniesFormProvider
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukErrorMessage, GovukHint, GovukInput, GovukLabel }
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import viewmodels.govuk.All.FluentInput
import viewmodels.govuk.input.InputViewModel

import java.time.LocalDate

class AssociatedCompaniesViewHelperSpec extends AnyFreeSpec with Matchers with ViewHelper {
  private implicit val messages: Messages = Helpers.stubMessages(messagesApi =
    Helpers.stubMessagesApi(
      messages = Map(
        "en" -> Map(
          "associatedCompanies.countLabel"      -> "Associated companies count",
          "associatedCompanies.heading"         -> "Associated companies",
          "associatedCompanies.heading.between" -> "Associated companies between {0} and {1}"
        )
      )
    )
  )
  val formProvider = new AssociatedCompaniesFormProvider()

  private val govukErrorMessage = new GovukErrorMessage
  private val govukHint = new GovukHint
  private val govukLabel = new GovukLabel
  private val govukInput = new GovukInput(govukErrorMessage, govukHint, govukLabel)

  "yesHtml" - {
    "should return input field when AskAssociatedCompaniesParameter is AskFull" in {
      val form = formProvider.apply()
      AssociatedCompaniesViewHelper.yesHtml(form, AskFull) shouldBe Some(
        govukInput(
          InputViewModel(
            field = form("associatedCompaniesCount"),
            label = Label(content = Text(messages("associatedCompanies.countLabel")))
          )
            .withCssClass("govuk-!-width-one-third")
            .withAttribute("maxlength" -> "2")
        )
      )
    }

    "should return input field when AskAssociatedCompaniesParameter is AskOnePart" in {
      val form = formProvider.apply()
      AssociatedCompaniesViewHelper.yesHtml(
        form,
        AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)))
      ) shouldBe Some(
        govukInput(
          InputViewModel(
            field = form("associatedCompaniesCount"),
            label = Label(content = Text("Associated companies count"))
          )
            .withCssClass("govuk-!-width-one-third")
            .withAttribute("maxlength" -> "2")
        )
      )
    }

    "should return None when AskAssociatedCompaniesParameter is DontAsk" in {
      val form = formProvider.apply()
      AssociatedCompaniesViewHelper.yesHtml(
        form,
        AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
      ) shouldBe None
    }
  }

  "heading" - {
    "should be Associated companies when AskAssociatedCompaniesParameter is AskFull" in {
      AssociatedCompaniesViewHelper.heading(AskFull) shouldBe h1("Associated companies", classes = "govuk-heading-xl")
    }

    "should be Associated companies when AskAssociatedCompaniesParameter is AskBothParts" in {
      AssociatedCompaniesViewHelper.heading(
        AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
      ) shouldBe h1("Associated companies", classes = "govuk-heading-xl")
    }

    "should be Associated companies when AskAssociatedCompaniesParameter is AskOnePart" in {
      AssociatedCompaniesViewHelper.heading(
        AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)))
      ) shouldBe h1("Associated companies between 1 January 1970 and 2 January 1970", classes = "govuk-heading-xl")
    }
  }
}
