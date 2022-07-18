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
import connectors.sharedmodel.{ AskBothParts, AskFull, AskOnePart, Period }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, AssociatedCompaniesFormProvider }
import models.{ AssociatedCompanies, NormalMode, UserAnswers }
import org.mockito.Mockito.when
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, TaxableProfitPage }
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.AssociatedCompaniesView

import java.time.LocalDate
import scala.concurrent.Future

class AssociatedCompaniesControllerSpec
    extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar with TableDrivenPropertyChecks {

  private lazy val associatedCompaniesRoute = routes.AssociatedCompaniesController.onPageLoad(NormalMode).url
  private val form = new AssociatedCompaniesFormProvider()()

  "AssociatedCompanies Controller" - {

    "GET page" - {

      "must return OK and the correct view for a GET" in {

        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- UserAnswers(userAnswersId)
                    .set(
                      AccountingPeriodPage,
                      AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(0).plusDays(1)))
                    )
            u2 <- u1.set(TaxableProfitPage, 1)
          } yield u2).toOption
        ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()
        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1),
          1.0,
          None
        )(*) returns Future.successful(AskFull)

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
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(
          userAnswers = (for {
            u1 <- UserAnswers(userAnswersId)
                    .set(
                      AccountingPeriodPage,
                      AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(0).plusDays(1)))
                    )
            u2 <- u1.set(TaxableProfitPage, 1)
            u3 <- u2.set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None))
          } yield u3).toOption
        ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()

        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1),
          1.0,
          None
        )(*) returns Future.successful(AskFull)

        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)

          val view = application.injector.instanceOf[AssociatedCompaniesView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view(
            form.fill(AssociatedCompaniesForm(AssociatedCompanies.Yes, Some(1), None, None)),
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

      "must throw RuntimeException if existing UserAnswer does not have AccountingPeriodPage and TaxableProfitPage values" in {
        val application = applicationBuilder(userAnswers = Some(UserAnswers(userAnswersId))).build()
        running(application) {
          val request = FakeRequest(GET, associatedCompaniesRoute)
          val result = route(application, request).value.failed.futureValue
          result mustBe a[RuntimeException]
          result.getMessage mustBe "Missing values for AccountingPeriodPage and(or) TaxableProfitPage"
        }
      }

      "must throw an Exception if associated parameters HTTP call fails" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]
        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1),
          1.0,
          None
        )(*) returns Future.failed(UpstreamErrorResponse("Bad request", 400))

        val application = applicationBuilder(userAnswers = (for {
          u1 <- UserAnswers(userAnswersId)
                  .set(
                    AccountingPeriodPage,
                    AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(0).plusDays(1)))
                  )
          u2 <- u1.set(TaxableProfitPage, 1)
        } yield u2).toOption)
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
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

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)
        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1),
          1.0,
          None
        )(*) returns Future.successful(AskFull)

        val application =
          applicationBuilder(
            userAnswers = (for {
              u1 <- UserAnswers(userAnswersId)
                      .set(
                        AccountingPeriodPage,
                        AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(0).plusDays(1)))
                      )
              u2 <- u1.set(TaxableProfitPage, 1)
            } yield u2).toOption
          ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, associatedCompaniesRoute)
              .withFormUrlEncodedBody(("associatedCompanies", "yes"), ("associatedCompaniesCount", "1"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ResultsPageController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when form parameters are invalid" in {
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application =
          applicationBuilder(
            userAnswers = (for {
              u1 <- UserAnswers(userAnswersId)
                      .set(
                        AccountingPeriodPage,
                        AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(0).plusDays(1)))
                      )
              u2 <- u1.set(TaxableProfitPage, 1)
            } yield u2).toOption
          ).overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
            .build()

        running(application) {

          val table = Table(
            ("requestParams", "errorKey", "associatedCompaniesParameter"),
            (Map("associatedCompanies" -> "invalid"), "associatedCompanies", AskFull),
            (Map("associatedCompanies" -> "yes"), "associatedCompaniesCount", AskFull),
            (
              Map("associatedCompanies" -> "yes"),
              "associatedCompaniesCount",
              AskOnePart(Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)))
            ),
            (
              Map("associatedCompanies" -> "yes"),
              "associatedCompaniesFY1Count",
              AskBothParts(
                Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)),
                Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1))
              )
            ),
            (
              Map("associatedCompanies" -> "yes", "associatedCompaniesFY1Count" -> "1"),
              "associatedCompaniesFY2Count",
              AskBothParts(
                Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1)),
                Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusDays(1))
              )
            )
          )

          forAll(table) { (requestParams, errorKey, associatedCompaniesParameter) =>
            mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
              accountingPeriodStart = LocalDate.ofEpochDay(0),
              accountingPeriodEnd = LocalDate.ofEpochDay(0).plusDays(1),
              1.0,
              None
            )(*) returns Future.successful(associatedCompaniesParameter)

            val request =
              FakeRequest(POST, associatedCompaniesRoute)
                .withFormUrlEncodedBody(
                  requestParams.toList: _*
                )

            val boundForm =
              if (errorKey == "associatedCompanies")
                form.bind(requestParams)
              else
                form.bind(requestParams).withError(errorKey, "associatedCompaniesCount.error.required")

            val view = application.injector.instanceOf[AssociatedCompaniesView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, associatedCompaniesParameter, NormalMode)(
              request,
              messages(application)
            ).toString
          }
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
    }
  }
}
