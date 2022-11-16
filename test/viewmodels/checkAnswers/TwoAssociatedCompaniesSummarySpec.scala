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

import connectors.sharedmodel.{ AskBothParts, DontAsk, Period }
import controllers.routes
import forms.{ AccountingPeriodForm, TwoAssociatedCompaniesForm }
import models.{ CheckMode, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.{ AccountingPeriodPage, TwoAssociatedCompaniesPage }
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

import java.time.LocalDate

class TwoAssociatedCompaniesSummarySpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  private implicit val messages: Messages = Helpers.stubMessages()
  private val askBothParts = AskBothParts(
    Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)),
    Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1))
  )

  "row" - {

    "when ask associated companies parameter is DontAsk, return empty" in {
      TwoAssociatedCompaniesSummary.row(UserAnswers("id"), DontAsk) shouldBe List.empty
    }

    "when ask associated companies parameter is AskBothParts and TwoAssociatedCompaniesPage is empty, return the summary row" in {
      TwoAssociatedCompaniesSummary.row(UserAnswers("id"), askBothParts) shouldBe List(
        SummaryListRowViewModel(
          key = "twoAssociatedCompanies.associatedCompanies",
          value = ValueViewModel("site.no"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("associatedCompanies.change.hidden")
          )
        )
      )
    }

    "when ask associated companies parameter is AskBothParts and TwoAssociatedCompaniesPage is non empty, return the summary rows" in {

      val table = Table(
        ("userAnswers", "fy1Count", "fy2Count"),
        (
          TwoAssociatedCompaniesForm(
            Some(1),
            None
          ),
          1,
          0
        ),
        (
          TwoAssociatedCompaniesForm(
            None,
            Some(1)
          ),
          0,
          1
        ),
        (
          TwoAssociatedCompaniesForm(
            Some(1),
            Some(2)
          ),
          1,
          2
        )
      )
      forAll(table) { (twoAssociatedCompaniesForm, fy1Count, fy2Count) =>
        TwoAssociatedCompaniesSummary.row(
          UserAnswers("id")
            .set(
              TwoAssociatedCompaniesPage,
              twoAssociatedCompaniesForm
            )
            .get
            .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
            .get,
          askBothParts
        ) shouldBe List(
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
            value = ValueViewModel(fy1Count.toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
            )
          ),
          SummaryListRowViewModel(
            key = "twoAssociatedCompanies.checkYourAnswersLabel",
            value = ValueViewModel(fy2Count.toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText("twoAssociatedCompanies.change.hidden")
            )
          )
        )
      }
    }
  }
}
