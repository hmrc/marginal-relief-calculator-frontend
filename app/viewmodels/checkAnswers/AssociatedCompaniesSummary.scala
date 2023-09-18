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

import models.associatedCompanies.{ AskBothParts, AssociatedCompaniesParameter, DontAsk }
import controllers.routes
import models.{ CheckMode, UserAnswers }
import pages.AssociatedCompaniesPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.All.FluentActionItem
import viewmodels.govuk.summarylist.{ ActionItemViewModel, SummaryListRowViewModel, ValueViewModel }
import viewmodels.implicits._

object AssociatedCompaniesSummary {

  def row(answers: UserAnswers, associatedCompaniesParameter: AssociatedCompaniesParameter)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    associatedCompaniesParameter match {
      case DontAsk | AskBothParts(_, _) => None
      case _ =>
        answers.get(AssociatedCompaniesPage).map { answer =>
          val count = answer.associatedCompaniesCount.getOrElse(0)
          SummaryListRowViewModel(
            key = "associatedCompanies.checkYourAnswersLabel",
            value = ValueViewModel(count.toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.AssociatedCompaniesController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("associatedCompanies.change.hidden"))
            )
          )
        }
    }

}
