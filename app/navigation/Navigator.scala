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

package navigation

import javax.inject.{ Inject, Singleton }
import models.{ Mode, UserAnswers }
import play.api.mvc.Call
import controllers.routes
import pages._
import models._

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case AccountingPeriodPage =>
      _ => routes.TaxableProfitController.onPageLoad(NormalMode)

    case TaxableProfitPage =>
      _ => routes.DistributionController.onPageLoad(NormalMode)

    case DistributionPage => distributionsNextRoute

    case DistributionsIncludedPage =>
      _ => routes.AssociatedCompaniesController.onPageLoad(NormalMode)

    case AssociatedCompaniesPage =>
      _ => routes.ResultsPageController.onPageLoad

    case _ =>
      _ => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = { case _ =>
    _ => routes.CheckYourAnswersController.onPageLoad
  }

  def distributionsNextRoute(answers: UserAnswers): Call =
    answers.get(DistributionPage) match {
      case Some(Distribution.Yes) => routes.DistributionsIncludedController.onPageLoad(NormalMode)
      case Some(Distribution.No)  => routes.AssociatedCompaniesController.onPageLoad(NormalMode)
      case _                      => routes.JourneyRecoveryController.onPageLoad()
    }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
