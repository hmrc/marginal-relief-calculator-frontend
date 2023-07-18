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
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

class FeedbackSurveyControllerSpec extends SpecBase with MockitoSugar {
  val app: Application = applicationBuilder().build()
  val config: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Feedback Survey Controller" - {

    "Redirect to the ExitSurvey and clear a session" in {
      running(app) {
        val request = FakeRequest(GET, routes.FeedbackSurveyController.redirectToExitSurvey.url)

        val result = route(app, request).value

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(config.exitSurveyUrl)
      }
    }
  }

}
