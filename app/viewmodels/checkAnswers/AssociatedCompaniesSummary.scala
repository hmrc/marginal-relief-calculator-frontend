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
import pages.AssociatedCompaniesPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AssociatedCompaniesSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AssociatedCompaniesPage).map { answer =>
      val count = answer.associatedCompaniesCount.orElse(answer.associatedCompaniesFY1Count).getOrElse(0)
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
