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

import connectors.sharedmodel.{ AskBothParts, AssociatedCompaniesParameter, DontAsk }
import controllers.routes
import forms.{ AccountingPeriodForm, DateUtils }
import models.{ CheckMode, UserAnswers }
import pages.{ AccountingPeriodPage, TwoAssociatedCompaniesPage }
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ Key, SummaryListRow }
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TwoAssociatedCompaniesSummary {
  def row(answers: UserAnswers, associatedCompaniesParameter: AssociatedCompaniesParameter)(implicit
    messages: Messages
  ): List[SummaryListRow] =
    associatedCompaniesParameter match {
      case DontAsk => List.empty
      case _: AskBothParts =>
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
                      DateUtils.financialYear(accountPeriodForm.accountingPeriodEndDateOrDefault).toString,
                      (DateUtils.financialYear(accountPeriodForm.accountingPeriodEndDateOrDefault) + 1).toString
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
            Some(
              List(
                SummaryListRowViewModel(
                  key = "twoAssociatedCompanies.associatedCompanies",
                  value = ValueViewModel(messages("site.no")),
                  actions = Seq(
                    ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                      .withVisuallyHiddenText(messages("associatedCompanies.change.hidden"))
                  )
                )
              )
            )
          }
        maybeRows.getOrElse(List.empty[SummaryListRow])
      case _ => List.empty
    }
}
