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

package controllers

import base.SpecBase
import forms.{ AccountingPeriodForm, DistributionFormProvider }
import models.{ Distribution, NormalMode }
import navigation.{ FakeNavigator, Navigator }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{ AccountingPeriodPage, DistributionPage, TaxableProfitPage }
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.DistributionView

import java.time.LocalDate
import scala.concurrent.Future

class DistributionControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val distributionRoute = routes.DistributionController.onPageLoad(NormalMode).url

  private val formProvider = new DistributionFormProvider()
  private val form = formProvider()
  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get

  "Distribution Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, distributionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DistributionView]

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view(form, NormalMode)(
          request,
          messages(application)
        ).toString.filterAndTrim
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = requiredAnswers.set(DistributionPage, Distribution.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, distributionRoute)

        val view = application.injector.instanceOf[DistributionView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view(form.fill(Distribution.values.head), NormalMode)(
          request,
          messages(application)
        ).toString.filterAndTrim
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, distributionRoute)
            .withFormUrlEncodedBody(("distribution", "yes"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and Distribution No" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, distributionRoute)
            .withFormUrlEncodedBody(("distribution", "no"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, distributionRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[DistributionView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result).filterAndTrim mustEqual view(boundForm, NormalMode)(
          request,
          messages(application)
        ).toString.filterAndTrim
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, distributionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if request parameters are missing in user answers" in {

      val application = applicationBuilder(userAnswers =
        Some(
          emptyUserAnswers
            .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
            .get
        )
      ).build()

      running(application) {
        val request = FakeRequest(GET, distributionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, distributionRoute)
            .withFormUrlEncodedBody(("value", Distribution.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if required parameters are missing in user answers" in {

      val application = applicationBuilder(userAnswers =
        Some(
          emptyUserAnswers
            .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
            .get
        )
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, distributionRoute)
            .withFormUrlEncodedBody(("value", Distribution.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
