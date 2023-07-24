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
import models.{ CheckMode, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.TaxableProfitPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.govuk.all.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits.{ stringToKey, stringToText }

class TaxableProfitSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()

  "row" - {
    "when answer available, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          TaxableProfitPage,
          1000
        )
        .get
      TaxableProfitSummary.row(userAnswers) shouldBe Some(
        SummaryListRowViewModel(
          key = "taxableProfit.checkYourAnswersLabel",
          value = ValueViewModel("£1,000"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TaxableProfitController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("taxableProfit.change.hidden")
          )
        )
      )
    }

    "when answer unavailable, return empty" in {
      val userAnswers = UserAnswers("id")
      TaxableProfitSummary.row(userAnswers) shouldBe None
    }
  }
}
