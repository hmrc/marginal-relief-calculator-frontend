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

package pages

import base.SpecBase
import handlers.ErrorHandler
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.NOT_FOUND
import play.api.test.FakeRequest
import play.api.test.Helpers.{ GET, contentAsString, defaultAwaitTimeout, running, status }
import views.html.{ ErrorTemplate, InternalServerErrorTemplate }

class ErrorHandlerSpec extends SpecBase with MockitoSugar {
  "Error handler renders view" in {
    val application = applicationBuilder(None).build()
    running(application) {

      val errorHandler = application.injector.instanceOf[ErrorHandler]
      val request = FakeRequest(GET, "/fake")
      val statusCode = NOT_FOUND
      val message = "Please check that you have entered the correct web address."

      val result = errorHandler.onClientError(request, statusCode.intValue, message)

      status(result) `mustEqual` 404

      val view = application.injector.instanceOf[ErrorTemplate]

      contentAsString(result).filterAndTrim `mustEqual` view
        .render("Page not found", "This page can’t be found", message, request, messages(application))
        .toString
        .filterAndTrim

    }
  }

  "Error handler 500" in {
    val application = applicationBuilder(None).build()
    running(application) {

      val errorHandler = application.injector.instanceOf[ErrorHandler]
      val request = FakeRequest(GET, "/fake")
      val result = errorHandler.onServerError(request, new Exception)

      status(result) `mustEqual` 500

      val view = application.injector.instanceOf[InternalServerErrorTemplate]

      contentAsString(result).filterAndTrim `mustEqual`
        view
          .render(
            request,
            messages(application)
          )
          .toString
          .filterAndTrim

    }
  }
}
