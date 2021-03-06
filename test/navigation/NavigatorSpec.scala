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

import base.SpecBase
import controllers.routes
import pages.{ DistributionPage, _ }
import models._

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from AccountingPeriod page to TaxableProfit page" in {
        navigator.nextPage(AccountingPeriodPage, NormalMode, UserAnswers("id")) mustBe routes.TaxableProfitController
          .onPageLoad(NormalMode)
      }

      "must go from TaxableProfit page to Distribution page" in {
        navigator.nextPage(TaxableProfitPage, NormalMode, UserAnswers("id")) mustBe routes.DistributionController
          .onPageLoad(NormalMode)
      }

      "must go from Distribution to Distributions Included page" - {
        "With Distribution Page Yes" in {
          val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.Yes).success.value
          navigator.distributionsNextRoute(userAnswers) mustBe routes.DistributionsIncludedController.onPageLoad(
            NormalMode
          )
        }

        "With Distribution Page No" in {
          val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.No).success.value
          navigator.distributionsNextRoute(userAnswers) mustBe routes.AssociatedCompaniesController.onPageLoad(
            NormalMode
          )
        }

        "With Distribution value not set" in {
          val userAnswers = UserAnswers("id")
          navigator.distributionsNextRoute(userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from Distribution Included page to Associated Companies page" in {
        navigator.nextPage(
          DistributionsIncludedPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe routes.AssociatedCompaniesController.onPageLoad(NormalMode)
      }

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from distribution to distributions included" in {
        val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.Yes).success.value
        navigator.nextPage(DistributionPage, CheckMode, userAnswers) mustBe routes.DistributionsIncludedController
          .onPageLoad(CheckMode)
      }

      "must go from distribution to CheckYourAnswers" in {
        val userAnswers = UserAnswers("id").set(DistributionPage, Distribution.No).success.value
        navigator.nextPage(DistributionPage, CheckMode, userAnswers) mustBe routes.CheckYourAnswersController.onPageLoad
      }

      "must go from distribution to JourneyRecoveryController when value not set" in {
        val userAnswers = UserAnswers("id")
        navigator.nextPage(DistributionPage, CheckMode, userAnswers) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }
    }
  }
}
