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
import controllers.routes
import forms.AccountingPeriodForm
import forms.DateUtils.DateOps
import models.{ CheckMode, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.AccountingPeriodPage
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.all.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits.{ stringToKey, stringToText }

import java.time.LocalDate

class AccountingPeriodSummarySpec extends AnyFreeSpec with Matchers {

  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private implicit val messages: Messages = Helpers.stubMessages()

  "row" - {
    "when answer available, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          AccountingPeriodPage,
          AccountingPeriodForm(
            epoch,
            Some(epoch.plusDays(1))
          )
        )
        .get
      AccountingPeriodSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "accountingPeriod.checkYourAnswersLabel",
          value = ValueViewModel(
            HtmlContent(
              messages(
                "site.from.to",
                epoch.formatDate,
                Some(epoch.plusDays(1).formatDate)
              )
            )
          ),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AccountingPeriodController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("accountingPeriodStartDate.change.hidden")
          )
        )
      )
    }

    "when end date unavailable, return the summary row with default date and message" in {
      val userAnswers = UserAnswers("id")
        .set(
          AccountingPeriodPage,
          AccountingPeriodForm(
            epoch,
            None
          )
        )
        .get
      AccountingPeriodSummary.row(userAnswers) shouldBe List(
        SummaryListRowViewModel(
          key = "accountingPeriod.checkYourAnswersLabel",
          value = ValueViewModel(
            HtmlContent(
              messages(
                "site.from.to",
                epoch.formatDate,
                s"${epoch.plusDays(1).formatDate}.</br>${messages("accountingPeriod.defaultedEndDateMessage")}"
              )
            )
          ),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AccountingPeriodController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("accountingPeriodStartDate.change.hidden")
          )
        )
      )
    }

    "when answer unavailable, return empty" in {
      val userAnswers = UserAnswers("id")
      AccountingPeriodSummary.row(userAnswers) shouldBe List.empty
    }
  }
}
