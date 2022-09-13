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

package viewmodels.checkAnswers

import controllers.routes
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, TwoAssociatedCompaniesForm }
import models.{ AssociatedCompanies, CheckMode, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, TwoAssociatedCompaniesPage }
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

import java.time.LocalDate

class AssociatedCompaniesSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()

  "row" - {
    "when AssociatedCompaniesPage answer available, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          AssociatedCompaniesPage,
          AssociatedCompaniesForm(
            AssociatedCompanies.Yes,
            Some(1)
          )
        )
        .get
      AssociatedCompaniesSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "associatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("1"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("associatedCompanies.change.hidden")
          )
        )
      )
    }

    "when TwoAssociatedCompaniesPage answer available, return the summary rows" in {
      val userAnswers = UserAnswers("id")
        .set(
          TwoAssociatedCompaniesPage,
          TwoAssociatedCompaniesForm(
            Some(1),
            Some(2)
          )
        )
        .get
        .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
        .get
      AssociatedCompaniesSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.associatedCompanies",
          value = ValueViewModel("site.yes"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("associatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("1"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("2"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        )
      )
    }

    "when TwoAssociatedCompaniesPage answer available and FY2 count is empty, return the summary rows" in {
      val userAnswers = UserAnswers("id")
        .set(
          TwoAssociatedCompaniesPage,
          TwoAssociatedCompaniesForm(
            Some(1),
            None
          )
        )
        .get
        .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
        .get
      AssociatedCompaniesSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.associatedCompanies",
          value = ValueViewModel("site.yes"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("associatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("1"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("0"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        )
      )
    }

    "when TwoAssociatedCompaniesPage answer available and FY1 count is empty, return the summary rows" in {
      val userAnswers = UserAnswers("id")
        .set(
          TwoAssociatedCompaniesPage,
          TwoAssociatedCompaniesForm(
            None,
            Some(1)
          )
        )
        .get
        .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
        .get
      AssociatedCompaniesSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.associatedCompanies",
          value = ValueViewModel("site.yes"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("associatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("0"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        ),
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.checkYourAnswersLabel",
          value = ValueViewModel("1"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
          )
        )
      )
    }
  }
}
