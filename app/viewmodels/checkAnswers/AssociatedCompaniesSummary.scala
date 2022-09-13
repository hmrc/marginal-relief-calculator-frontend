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
import forms.{ AccountingPeriodForm, DateUtils }
import models.{ CheckMode, UserAnswers }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, TwoAssociatedCompaniesPage }
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ Key, SummaryListRow }
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AssociatedCompaniesSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): List[SummaryListRow] = {
    val maybeRows: Option[List[SummaryListRow]] =
      answers.get(TwoAssociatedCompaniesPage).map[List[SummaryListRow]] { answer =>
        val accountPeriodForm: AccountingPeriodForm = answers.get(AccountingPeriodPage).get
        val associatedCompaniesFY1Count = answer.associatedCompaniesFY1Count.getOrElse(0)
        val associatedCompaniesFY2Count = answer.associatedCompaniesFY2Count.getOrElse(0)
        List(
          SummaryListRowViewModel(
            key = "twoAssociatedCompanies.associatedCompanies",
            value = ValueViewModel(messages("site.yes")),
            actions = Seq(
              ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("associatedCompanies.change.hidden"))
            )
          ),
          SummaryListRowViewModel(
            key = Key(content =
              Text(
                messages(
                  "twoAssociatedCompanies.checkYourAnswersLabel",
                  DateUtils.financialYear(accountPeriodForm.accountingPeriodStartDate).toString,
                  (DateUtils.financialYear(accountPeriodForm.accountingPeriodStartDate) + 1).toString
                )
              )
            ),
            value = ValueViewModel(associatedCompaniesFY1Count.toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("twoAssociatedCompanies.change.hidden"))
            )
          ),
          SummaryListRowViewModel(
            key = Key(content =
              Text(
                messages(
                  "twoAssociatedCompanies.checkYourAnswersLabel",
                  DateUtils.financialYear(accountPeriodForm.accountingPeriodEndDate.get).toString,
                  (DateUtils.financialYear(accountPeriodForm.accountingPeriodEndDate.get) + 1).toString
                )
              )
            ),
            value = ValueViewModel(associatedCompaniesFY2Count.toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.TwoAssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("twoAssociatedCompanies.change.hidden"))
            )
          )
        )
      } orElse {
        answers.get(AssociatedCompaniesPage).map[List[SummaryListRow]] { answer =>
          val count = answer.associatedCompaniesCount.getOrElse(0)
          List(
            SummaryListRowViewModel(
              key = "associatedCompanies.checkYourAnswersLabel",
              value = ValueViewModel(count.toString),
              actions = Seq(
                ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                  .withVisuallyHiddenText(messages("associatedCompanies.change.hidden"))
              )
            )
          )
        }
      }
    maybeRows.getOrElse(List.empty[SummaryListRow])
  }
}
