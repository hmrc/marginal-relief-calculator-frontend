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
import connectors.MarginalReliefCalculatorConnector
import forms._
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, NormalMode }
import navigation.{ FakeNavigator, Navigator }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.PDFAddCompanyDetailsView

import java.time.LocalDate
import scala.concurrent.Future

class PDFAddCompanyDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  private val formProvider = new PDFAddCompanyDetailsFormProvider()
  private val form = formProvider()
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private val addCompanyDetailsRoute = routes.PDFAddCompanyDetailsController.onPageLoad().url

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(epoch, Some(epoch.plusDays(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.Yes)
    .get
    .set(
      DistributionsIncludedPage,
      DistributionsIncludedForm(
        DistributionsIncluded.Yes,
        Some(1)
      )
    )
    .get
    .set(
      AssociatedCompaniesPage,
      AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))
    )
    .get

  "PDFMetadata Controller" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, addCompanyDetailsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[PDFAddCompanyDetailsView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(form, NormalMode)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, addCompanyDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        val mockConnector = mock[MarginalReliefCalculatorConnector]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockConnector, mockSessionRepository)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, addCompanyDetailsRoute)
              .withFormUrlEncodedBody(("pdfAddCompanyDetails", "yes"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, addCompanyDetailsRoute)
              .withFormUrlEncodedBody("companyName" -> "A" * 161, "utr" -> "1" * 16)

          val boundForm = form
            .bind(Map("companyName" -> "A" * 161, "utr" -> "1" * 16))

          val view = application.injector.instanceOf[PDFAddCompanyDetailsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result).filterAndTrim mustEqual view(boundForm, NormalMode)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, addCompanyDetailsRoute)
              .withFormUrlEncodedBody(("pdfAddCompanyDetails", "yes"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
