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

import models.{ CheckMode, UserAnswers }
import pages.InputScreenPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._

import java.text.NumberFormat
import java.util.Locale
import controllers.routes
import viewmodels.implicits._

// $COVERAGE-OFF$
object InputScreenSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): List[SummaryListRow] =
    answers
      .get(InputScreenPage)
      .map { answer =>
        List(
          SummaryListRowViewModel(
            key = "distribution.checkYourAnswersLabel",
            value = ValueViewModel(s"£${NumberFormat.getNumberInstance(Locale.UK).format(answer.distribution)}"),
            actions = Seq(
              ActionItemViewModel("site.change", routes.InputScreenController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("distribution.change.hidden"))
            )
          )
        )
      }
      .getOrElse(List.empty)
}
// $COVERAGE-ON$
