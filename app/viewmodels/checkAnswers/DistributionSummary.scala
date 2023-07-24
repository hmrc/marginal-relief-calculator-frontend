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
import models.{ CheckMode, Distribution, UserAnswers }
import pages.{ DistributionPage, DistributionsIncludedPage }
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.All.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits._

import java.text.NumberFormat
import java.util.Locale

object DistributionSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DistributionPage).map { answer =>
      val value = if (answer == Distribution.Yes) {
        answers
          .get(DistributionsIncludedPage)
          .flatMap(
            _.distributionsIncludedAmount
              .filter(_ > 0)
              .map(a => s"£${NumberFormat.getNumberInstance(Locale.UK).format(a)}")
          ) getOrElse messages("distributionsIncluded.emptyValue")
      } else {
        messages("distributionsIncluded.emptyValue")
      }

      SummaryListRowViewModel(
        key = "distributionsIncluded.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DistributionController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("distributionsIncluded.change.hidden"))
        )
      )
    }
}
