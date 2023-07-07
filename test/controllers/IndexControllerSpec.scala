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

package controllers

import base.SpecBase
import forms.AccountingPeriodForm
import models.NormalMode
import pages.AccountingPeriodPage
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, redirectLocation, route, running, status, writeableOf_AnyContentAsEmpty}
import repositories.SessionRepository
import uk.gov.hmrc.http.SessionKeys
import views.html.IndexView

import java.time.LocalDate

class IndexControllerSpec extends SpecBase {

  private val epoch: LocalDate = LocalDate.ofEpochDay(0)

  "Index Controller" - {

    "GET /" - {
      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    "GET /start" - {

      "must create empty user answers when missing and redirect to /accounting-period" in {

        val application = applicationBuilder(userAnswers = None).build()
        val sessionRepository = application.injector.instanceOf[SessionRepository]

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onStart().url)
            .withSession(SessionKeys.sessionId -> "test-session-id")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.AccountingPeriodController.onPageLoad(NormalMode).url)
          val userAnswers = sessionRepository
            .get("test-session-id")
            .futureValue
          userAnswers.get.id mustBe "test-session-id"
          userAnswers.get.data mustBe Json.obj()
        }
      }

      "must clear user answers when present and redirect to /accounting-period" in {

        val application = applicationBuilder(userAnswers =
          Some(emptyUserAnswers.set(AccountingPeriodPage, AccountingPeriodForm(epoch, Some(epoch))).get)
        ).build()
        val sessionRepository = application.injector.instanceOf[SessionRepository]

        running(application) {
          val request = FakeRequest(GET, routes.IndexController.onStart().url)
            .withSession(SessionKeys.sessionId -> "test-session-id")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustBe Some(routes.AccountingPeriodController.onPageLoad(NormalMode).url)
          val userAnswers = sessionRepository
            .get("test-session-id")
            .futureValue
          userAnswers.get.id mustBe "test-session-id"
          userAnswers.get.data mustBe Json.obj()
        }
      }
    }
  }
}
