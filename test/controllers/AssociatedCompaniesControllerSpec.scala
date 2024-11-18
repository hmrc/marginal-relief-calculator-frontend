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
import models.associatedCompanies._
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, AssociatedCompaniesFormProvider, DistributionsIncludedForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded, NormalMode }
import org.mockito.Mockito.when
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import uk.gov.hmrc.http.{ SessionKeys, UpstreamErrorResponse }
import views.html.AssociatedCompaniesView

import java.time.LocalDate
import scala.concurrent.Future

class AssociatedCompaniesControllerSpec
    extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar with TableDrivenPropertyChecks {

  private lazy val associatedCompaniesRoute = routes.AssociatedCompaniesController.onPageLoad(NormalMode).url
  private val form = new AssociatedCompaniesFormProvider()()

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.Yes)
    .get
    .set(DistributionsIncludedPage, DistributionsIncludedForm(DistributionsIncluded.Yes, Some(1)))
    .get

  "AssociatedCompanies Controller" - {
    "GET page" - {
      "must return OK and the correct view for a GET" in {
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(Some(requiredAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        mockParameterService
          .associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
          .returns(Future.successful(AskFull))

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AssociatedCompaniesView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(form, AskFull, NormalMode)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must return OK when distributions is yes and distributions included is non-empty" in {
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(
          Some(
            emptyUserAnswers
              .set(
                page = AccountingPeriodPage,
                value = AccountingPeriodForm(
                  accountingPeriodStartDate = LocalDate.ofEpochDay(0),
                  accountingPeriodEndDate = Some(LocalDate.ofEpochDay(1))
                )
              )
              .get
              .set(page = TaxableProfitPage, value = 1)
              .get
              .set(page = DistributionPage, value = Distribution.Yes)
              .get
              .set(
                page = DistributionsIncludedPage,
                value = DistributionsIncludedForm(
                  distributionsIncluded = DistributionsIncluded.Yes,
                  distributionsIncludedAmount = Some(1)
                )
              )
              .get
          )
        )
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        mockParameterService
          .associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
          .returns(Future.successful(AskFull))

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[AssociatedCompaniesView]

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(form, AskFull, NormalMode)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(
          userAnswers = (for {
            u2 <- requiredAnswers.set(TaxableProfitPage, 1)
            u3 <- u2.set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1)))
          } yield u3).toOption
        ).overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
        ) returns Future.successful(AskFull)

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)

          val view = application.injector.instanceOf[AssociatedCompaniesView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(
            form.fill(AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1))),
            AskFull,
            NormalMode
          )(request, messages(application)).toString.filterAndTrim
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if required params are not found in user answers" in {
        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
              .get
          )
        ).build()
        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)

          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to CheckYourAnswers page, if AssociatedCompanies parameter is DontAsk" in {
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        mockParameterService
          .associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
          )
          .returns(Future.successful(DontAsk))

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
        }
      }

      "must throw an Exception if associated parameters HTTP call fails" in {

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
        ) returns Future.failed(UpstreamErrorResponse("Bad request", 400))

        val application = applicationBuilder(Some(requiredAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)
          val result = route(application, request).value.failed.futureValue
          result mustBe a[UpstreamErrorResponse]
          result.getMessage mustBe "Bad request"
        }
      }
    }

    "POST page" - {
      "must redirect to CheckYourAnswersController page when valid data is submitted" in {
        val mockSessionRepository = mock[SessionRepository]
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
        ) returns Future.successful(AskFull)

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "yes"), ("associatedCompaniesCount", "1"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
        }
      }

      "must clear associated companies count when associatedCompanies is No" in {

        val mockSessionRepository = mock[SessionRepository]

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        when(
          mockSessionRepository.set(
            requiredAnswers
              .set(
                AssociatedCompaniesPage,
                AssociatedCompaniesForm(
                  AssociatedCompanies.No,
                  None
                )
              )
              .get
          )
        ) thenReturn Future.successful(true)
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
        ) returns Future.successful(AskFull)

        val application =
          applicationBuilder(userAnswers =
            Some(
              requiredAnswers
                .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1)))
                .get
            )
          )
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "no"))
              .withSession(SessionKeys.sessionId -> "test-session-id")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
        }
      }

      "must redirect to the TwoAssociatedCompanies page when valid data is submitted and ask associated companies parameter is AskBothParts" in {

        val mockSessionRepository = mock[SessionRepository]

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1)
        ) returns Future.successful(
          AskBothParts(
            Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)),
            Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1))
          )
        )

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "yes"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.TwoAssociatedCompaniesController.onPageLoad(NormalMode).url
        }
      }

      "must return a Bad Request when associated companies requirement is AskFull or AskOnePart, but associatedCompaniesCount is empty" in {

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application =
          applicationBuilder(Some(requiredAnswers))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        running(application) {

          val table = Table(
            ("requestParams", "associatedCompaniesParameter"),
            (Map("associatedCompanies" -> "yes"), AskFull),
            (
              Map("associatedCompanies" -> "yes"),
              AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)))
            )
          )

          forAll(table) { (requestParams, associatedCompaniesParameter) =>
            mockParameterService.associatedCompaniesParameters(
              accountingPeriodStart = LocalDate.ofEpochDay(0),
              accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
            ) returns Future.successful(associatedCompaniesParameter)

            val request =
              FakeRequest(POST, associatedCompaniesRoute)
                .withFormUrlEncodedBody(
                  requestParams.toList: _*
                )

            val boundForm =
              form.bind(requestParams).withError("associatedCompaniesCount", "associatedCompaniesCount.error.required")

            val view = application.injector.instanceOf[AssociatedCompaniesView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result).filterAndTrim mustEqual view(boundForm, associatedCompaniesParameter, NormalMode)(
              request,
              messages(application)
            ).toString.filterAndTrim
          }
        }
      }

      "must return a Bad Request when associatedCompanies parameter is invalid" in {

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1)
        ) returns Future.successful(AskFull)
        val application =
          applicationBuilder(Some(requiredAnswers))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        running(application) {

          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(
                "associatedCompanies" -> "invalid"
              )

          val boundForm = form
            .withError("associatedCompanies", "associatedCompanies.error.invalid")

          val view = application.injector.instanceOf[AssociatedCompaniesView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result).filterAndTrim mustEqual view(boundForm, AskFull, NormalMode)(
            request,
            messages(application)
          ).toString.filterAndTrim
        }
      }

      "redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "yes"), ("associatedCompaniesCount", "1"))

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
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "yes"), ("associatedCompaniesCount", "1"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
