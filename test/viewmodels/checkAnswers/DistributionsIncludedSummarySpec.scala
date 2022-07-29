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
import forms.DistributionsIncludedForm
import models.{ CheckMode, DistributionsIncluded, UserAnswers }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.DistributionsIncludedPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

import scala.xml.Null

class DistributionsIncludedSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()

  "row" - {
    "when answer available, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          DistributionsIncludedPage,
          DistributionsIncludedForm(
            DistributionsIncluded.Yes,
            Some(1000)
          )
        )
        .get
      DistributionsIncludedSummary.row(userAnswers) shouldBe Some(
        SummaryListRowViewModel(
          key = "distributionsIncluded.checkYourAnswersLabel",
          value = ValueViewModel("£1,000"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DistributionController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("distributionsIncluded.change.hidden")
          )
        )
      )
    }

    "when answer available but no amount, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          DistributionsIncludedPage,
          DistributionsIncludedForm(
            DistributionsIncluded.Yes,
            None
          )
        )
        .get
      DistributionsIncludedSummary.row(userAnswers) shouldBe Some(
        SummaryListRowViewModel(
          key = "distributionsIncluded.checkYourAnswersLabel",
          value = ValueViewModel(messages("distributionsIncluded.EmptyValue")),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DistributionController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("distributionsIncluded.change.hidden")
          )
        )
      )
    }

    "when answer available but amount 0, return the summary row" in {
      val userAnswers = UserAnswers("id")
        .set(
          DistributionsIncludedPage,
          DistributionsIncludedForm(
            DistributionsIncluded.Yes,
            Some(0)
          )
        )
        .get
      DistributionsIncludedSummary.row(userAnswers) shouldBe Some(
        SummaryListRowViewModel(
          key = "distributionsIncluded.checkYourAnswersLabel",
          value = ValueViewModel(messages("distributionsIncluded.EmptyValue")),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DistributionController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText("distributionsIncluded.change.hidden")
          )
        )
      )
    }

    "when answer unavailable, return empty" in {
      val userAnswers = UserAnswers("id")
      DistributionsIncludedSummary.row(userAnswers) shouldBe None
    }
  }
}
