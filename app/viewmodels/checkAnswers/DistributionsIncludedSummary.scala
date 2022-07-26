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
import models.{ CheckMode, UserAnswers }
import pages.DistributionsIncludedPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

import java.text.NumberFormat
import java.util.Locale

object DistributionsIncludedSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DistributionsIncludedPage).map { answer =>
      val value = answer.distributionsIncludedAmount map (amount =>
        if (amount > 0) {
          s"£${NumberFormat.getNumberInstance(Locale.UK).format(amount)}"
        } else {
          messages("distributionsIncluded.EmptyValue")
        }
      ) getOrElse messages("distributionsIncluded.EmptyValue")

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
