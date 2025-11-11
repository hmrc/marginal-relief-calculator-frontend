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
import forms._
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded }
import navigation.{ FakeNavigator, Navigator }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import views.html.PDFMetadataView

import java.time.LocalDate
import scala.concurrent.Future

class PDFMetadataControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  private val formProvider = new PDFMetadataFormProvider()
  private val form = formProvider()
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private val pDFMetadataRoute = routes.PDFMetadataController.onPageLoad().url

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
        val application: Application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, pDFMetadataRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[PDFMetadataView]

          status(result) `mustEqual` OK
          contentAsString(result).filterAndTrim `mustEqual` view(form)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val StringUTR = "123456789112345L"

        val userAnswers =
          requiredAnswers.set(PDFMetadataPage, PDFMetadataForm(Some("name"), Some(StringUTR))).get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, pDFMetadataRoute)

          val view = application.injector.instanceOf[PDFMetadataView]

          val result = route(application, request).value

          status(result) `mustEqual` OK
          contentAsString(result).filterAndTrim `mustEqual` view(
            form.fill(PDFMetadataForm(Some("name"), Some(StringUTR)))
          )(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, pDFMetadataRoute)

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value `mustEqual` routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        when(mockSessionRepository.set(any())) `thenReturn` Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers))
            .overrides(
              bind[Navigator].toInstance(
                new FakeNavigator(
                  desiredRoute = onwardRoute,
                  associatedCompaniesParameterService = mockParameterService,
                  sessionRepository = mockSessionRepository
                )
              ),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, pDFMetadataRoute)
              .withFormUrlEncodedBody(("companyName", "name"), ("utr", "1234567891"))

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value `mustEqual` onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(requiredAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, pDFMetadataRoute)
              .withFormUrlEncodedBody("companyName" -> "A" * 161, "utr" -> "1" * 16)

          val boundForm = form
            .bind(Map("companyName" -> "A" * 161, "utr" -> "1" * 16))

          val view = application.injector.instanceOf[PDFMetadataView]

          val result = route(application, request).value

          status(result) `mustEqual` BAD_REQUEST
          contentAsString(result).filterAndTrim `mustEqual` view(boundForm)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, pDFMetadataRoute)
              .withFormUrlEncodedBody(("companyName", "name"), ("utr", "12345"))

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value `mustEqual` routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
