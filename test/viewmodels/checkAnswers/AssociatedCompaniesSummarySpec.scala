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

package viewmodels.checkAnswers

import connectors.sharedmodel.{ AskBothParts, AskFull, AskOnePart, DontAsk, Period }
import controllers.routes
import forms.AssociatedCompaniesForm
import models.{ AssociatedCompanies, CheckMode, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.AssociatedCompaniesPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.govuk.all.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits.{ stringToKey, stringToText }

import java.time.LocalDate

class AssociatedCompaniesSummarySpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  private implicit val messages: Messages = Helpers.stubMessages()

  "row" - {
    "when AskAssociatedCompaniesParameter is AskBothParts, return empty" in {
      val askAssociatedCompaniesParameter = AskBothParts(
        Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)),
        Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1))
      )
      val userAnswers = UserAnswers("id")
        .set(
          AssociatedCompaniesPage,
          AssociatedCompaniesForm(
            AssociatedCompanies.Yes,
            None
          )
        )
        .get
      AssociatedCompaniesSummary.row(userAnswers, askAssociatedCompaniesParameter) shouldBe None
    }

    "when AskAssociatedCompaniesParameter is DontAsk, return empty" in {
      AssociatedCompaniesSummary.row(UserAnswers("id"), DontAsk) shouldBe None
    }

    "when AssociatedCompaniesParameter is AskFul or AskOnePart, return the summary row" in {
      val table = Table(
        ("associatedCompaniesParameter", "userAnswers", "expected"),
        (
          AskFull,
          UserAnswers("id")
            .set(
              AssociatedCompaniesPage,
              AssociatedCompaniesForm(
                AssociatedCompanies.Yes,
                Some(1)
              )
            )
            .get,
          Some(
            SummaryListRowViewModel(
              key = "associatedCompanies.checkYourAnswersLabel",
              value = ValueViewModel("1"),
              actions = Seq(
                ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                  .withVisuallyHiddenText("associatedCompanies.change.hidden")
              )
            )
          )
        ),
        (
          AskFull,
          UserAnswers("id")
            .set(
              AssociatedCompaniesPage,
              AssociatedCompaniesForm(
                AssociatedCompanies.No,
                None
              )
            )
            .get,
          Some(
            SummaryListRowViewModel(
              key = "associatedCompanies.checkYourAnswersLabel",
              value = ValueViewModel("0"),
              actions = Seq(
                ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                  .withVisuallyHiddenText("associatedCompanies.change.hidden")
              )
            )
          )
        ),
        (
          AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0))),
          UserAnswers("id")
            .set(
              AssociatedCompaniesPage,
              AssociatedCompaniesForm(
                AssociatedCompanies.Yes,
                Some(1)
              )
            )
            .get,
          Some(
            SummaryListRowViewModel(
              key = "associatedCompanies.checkYourAnswersLabel",
              value = ValueViewModel("1"),
              actions = Seq(
                ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                  .withVisuallyHiddenText("associatedCompanies.change.hidden")
              )
            )
          )
        ),
        (
          AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0))),
          UserAnswers("id")
            .set(
              AssociatedCompaniesPage,
              AssociatedCompaniesForm(
                AssociatedCompanies.No,
                None
              )
            )
            .get,
          Some(
            SummaryListRowViewModel(
              key = "associatedCompanies.checkYourAnswersLabel",
              value = ValueViewModel("0"),
              actions = Seq(
                ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                  .withVisuallyHiddenText("associatedCompanies.change.hidden")
              )
            )
          )
        )
      )
      forAll(table) { (associatedParameter, userAnswers, expected) =>
        AssociatedCompaniesSummary.row(userAnswers, associatedParameter) shouldBe expected
      }
    }
  }
}
