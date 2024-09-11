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

import models.associatedCompanies.{AskBothParts, AskFull, AskOnePart, DontAsk, Period}
import forms.AssociatedCompaniesFormProvider
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukErrorMessage, GovukHint, GovukInput, GovukLabel}
import uk.gov.hmrc.govukfrontend.views.html.helpers.{GovukFormGroup, GovukHintAndErrorMessage}
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
          "associatedCompanies.heading.between" -> "Associated companies between {0} and {1}",
          "date.1"                              -> "January",
          "date.2"                              -> "February",
          "date.3"                              -> "March",
          "date.4"                              -> "April",
          "date.5"                              -> "May",
          "date.6"                              -> "June",
          "date.7"                              -> "July",
          "date.8"                              -> "August",
          "date.9"                              -> "September",
          "date.10"                             -> "October",
          "date.11"                             -> "November",
          "date.12"                             -> "December"
        )
      )
    )
  )
  val formProvider = new AssociatedCompaniesFormProvider()

  private val govukErrorMessage = new GovukErrorMessage
  private val govukHint = new GovukHint
  private val govukHintAndErrorMessage = new GovukHintAndErrorMessage(govukHint, govukErrorMessage)
  private val govukFormGroup = new GovukFormGroup
  private val govukLabel = new GovukLabel
  private val govukInput = new GovukInput(govukLabel, govukFormGroup, govukHintAndErrorMessage)

  "yesHtml" - {
    "should return input field when AskAssociatedCompaniesParameter is AskFull" in {
      val form = formProvider.apply()
      AssociatedCompaniesViewHelper.yesHtml(form, AskFull) shouldBe Some(
        govukInput(
          InputViewModel(
            field = form("associatedCompaniesCount"),
            label = Label(content = Text(messages("associatedCompanies.countLabel")))
          )
            .withCssClass("govuk-input--width-2")
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
            .withCssClass("govuk-input--width-2")
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
      AssociatedCompaniesViewHelper.heading(AskFull) shouldBe "Associated companies"
    }

    "should be Associated companies when AskAssociatedCompaniesParameter is AskBothParts" in {
      AssociatedCompaniesViewHelper.heading(
        AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
      ) shouldBe "Associated companies"
    }

    "should be Associated companies when AskAssociatedCompaniesParameter is AskOnePart" in {
      AssociatedCompaniesViewHelper.heading(
        AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)))
      ) shouldBe "Associated companies between 1 January 1970 and 2 January 1970"
    }

    "should be empty when AskAssociatedCompaniesParameter is DontAsk" in {
      AssociatedCompaniesViewHelper.heading(DontAsk) shouldBe ""
    }
  }
}
