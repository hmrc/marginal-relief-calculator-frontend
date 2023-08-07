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
import forms.{ AccountingPeriodForm, TaxableProfitFormProvider }
import models.NormalMode
import navigation.{ FakeNavigator, Navigator }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ AccountingPeriodPage, TaxableProfitPage }
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import views.html.TaxableProfitView

import java.time.LocalDate
import scala.concurrent.Future

class TaxableProfitControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new TaxableProfitFormProvider()
  val form: Form[Int] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer = 0

  private lazy val taxableProfitRoute = routes.TaxableProfitController.onPageLoad(NormalMode).url

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
    .get

  "TaxableProfit Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxableProfitRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaxableProfitView]

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view
          .render(form, NormalMode, request, messages(application))
          .toString
          .filterAndTrim
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredAnswers.set(TaxableProfitPage, validAnswer).get

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxableProfitRoute)

        val view = application.injector.instanceOf[TaxableProfitView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view(form.fill(validAnswer), NormalMode)(
          request,
          messages(application)
        ).toString.filterAndTrim
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockParameterService, mockSessionRepository)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxableProfitRoute)
            .withFormUrlEncodedBody(("value", "1"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, taxableProfitRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TaxableProfitView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, taxableProfitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if required params is missing in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxableProfitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, taxableProfitRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if required params is missing in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, taxableProfitRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
