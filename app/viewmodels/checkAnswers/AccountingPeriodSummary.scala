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
import forms.DateUtils.DateOps
import models.{ CheckMode, UserAnswers }
import pages.AccountingPeriodPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.All.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits._

object AccountingPeriodSummary {
  def row(answers: UserAnswers)(implicit messages: Messages): List[SummaryListRow] =
    answers
      .get(AccountingPeriodPage)
      .map { answer =>
        List(
          SummaryListRowViewModel(
            key = "accountingPeriod.checkYourAnswersLabel",
            value = ValueViewModel(
              HtmlContent(
                messages(
                  "site.from.to",
                  answer.accountingPeriodStartDate.govDisplayFormat,
                  answer.accountingPeriodEndDateOrDefault.govDisplayFormat + {
                    answer.accountingPeriodEndDate match {
                      case None    => ".<br>" + messages("accountingPeriod.defaultedEndDateMessage")
                      case Some(_) => ""
                    }
                  }
                )
              )
            ),
            actions = Seq(
              ActionItemViewModel("site.change", routes.AccountingPeriodController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("accountingPeriodStartDate.change.hidden"))
            )
          )
        )
      }
      .getOrElse(List.empty)
}
