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
import connectors.sharedmodel.{ AskBothParts, Period }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, TwoAssociatedCompaniesForm, TwoAssociatedCompaniesFormProvider }
import models.{ AssociatedCompanies, Distribution, NormalMode }
import org.mockito.Mockito.when
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.prop.TableDrivenPropertyChecks
import pages._
import play.api.data.FormError
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.TwoAssociatedCompaniesView

import java.time.LocalDate
import scala.concurrent.Future

class TwoAssociatedCompaniesControllerSpec
    extends SpecBase with IdiomaticMockito with ArgumentMatchersSugar with TableDrivenPropertyChecks {

  private val formProvider = new TwoAssociatedCompaniesFormProvider()
  private val form = formProvider()

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.No)
    .get
    .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, None))
    .get

  lazy val twoAssociatedCompaniesRoute = routes.TwoAssociatedCompaniesController.onPageLoad(NormalMode).url

  "TwoAssociatedCompanies Controller" - {

    "GET page" - {

      "must return OK and the correct view for a GET" in {

        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1)))
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]
        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()
        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1),
          1.0,
          None
        )(*) returns Future.successful(askParameter)

        running(application) {
          val request = FakeRequest(GET, twoAssociatedCompaniesRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[TwoAssociatedCompaniesView]
          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(form, accountingPeriodForm, askParameter, NormalMode, request, messages(application))
            .toString
            .filterAndTrim
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val valid = TwoAssociatedCompaniesForm(Some(1), Some(1))
        val userAnswers = requiredAnswers.set(TwoAssociatedCompaniesPage, valid).success.value
        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1)))
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector))
          .build()
        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1),
          1.0,
          None
        )(*) returns Future.successful(askParameter)

        running(application) {
          val request = FakeRequest(GET, twoAssociatedCompaniesRoute)

          val view = application.injector.instanceOf[TwoAssociatedCompaniesView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).filterAndTrim mustEqual view
            .render(
              form.fill(valid),
              accountingPeriodForm,
              askParameter,
              NormalMode,
              request,
              messages(application)
            )
            .toString
            .filterAndTrim
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, twoAssociatedCompaniesRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "POST" - {

      "must redirect to check your answers page, when valid data is submitted" in {

        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )

        val mockSessionRepository = mock[SessionRepository]
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1),
          1.0,
          None
        )(*) returns Future.successful(askParameter)
        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, twoAssociatedCompaniesRoute)
              .withFormUrlEncodedBody("associatedCompaniesFY1Count" -> "1", "associatedCompaniesFY2Count" -> "1")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad.url
        }
      }

      "must return a Bad Request and errors when form data binding fails" in {

        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1)))
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )

        val mockSessionRepository = mock[SessionRepository]
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1),
          1.0,
          None
        )(*) returns Future.successful(askParameter)
        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, twoAssociatedCompaniesRoute)
              .withFormUrlEncodedBody(
                "associatedCompaniesFY1Count" -> "invalid value",
                "associatedCompaniesFY2Count" -> "invalid value"
              )

          val boundForm = form.bind(
            Map("associatedCompaniesFY1Count" -> "invalid value", "associatedCompaniesFY2Count" -> "invalid value")
          )

          val view = application.injector.instanceOf[TwoAssociatedCompaniesView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view
            .render(boundForm, accountingPeriodForm, askParameter, NormalMode, request, messages(application))
            .toString
        }
      }

      "must return a Bad Request when form data fails validation" in {

        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1)))
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )

        val mockSessionRepository = mock[SessionRepository]
        val mockMarginalReliefCalculatorConnector: MarginalReliefCalculatorConnector =
          mock[MarginalReliefCalculatorConnector]

        mockMarginalReliefCalculatorConnector.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1),
          1.0,
          None
        )(*) returns Future.successful(askParameter)
        when(mockSessionRepository.set(*)) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MarginalReliefCalculatorConnector].toInstance(mockMarginalReliefCalculatorConnector)
          )
          .build()

        running(application) {

          val table = Table(
            ("scenario", "formInput", "errors"),
            (
              "Both counts a 0",
              Map("associatedCompaniesFY1Count" -> "0", "associatedCompaniesFY2Count" -> "0"),
              List("twoAssociatedCompanies.error.enterAtLeastOneValueGreaterThan0")
            ),
            (
              "Both counts are empty",
              Map.empty[String, String],
              List("twoAssociatedCompanies.error.enterAtLeastOneAnswer")
            ),
            (
              "associatedCompaniesFY1Count is 0 and associatedCompaniesFY2Count is empty",
              Map("associatedCompaniesFY1Count" -> "0"),
              List("twoAssociatedCompanies.error.enterAtLeastOneValueGreaterThan0")
            ),
            (
              "associatedCompaniesFY1Count is empty and associatedCompaniesFY2Count is 0",
              Map("associatedCompaniesFY2Count" -> "0"),
              List("twoAssociatedCompanies.error.enterAtLeastOneValueGreaterThan0")
            )
          )
          forAll(table) { (_, formInput, errors) =>
            val request =
              FakeRequest(POST, twoAssociatedCompaniesRoute)
                .withFormUrlEncodedBody(
                  formInput.toList: _*
                )

            val boundForm = form
              .bind(formInput)
              .copy(errors = errors.map(e => FormError("associatedCompaniesFY1Count", e)))

            val view = application.injector.instanceOf[TwoAssociatedCompaniesView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result).filterAndTrim mustEqual view
              .render(
                boundForm,
                accountingPeriodForm,
                askParameter,
                NormalMode,
                request,
                messages(application)
              )
              .toString
              .filterAndTrim
          }
        }
      }

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, twoAssociatedCompaniesRoute)
              .withFormUrlEncodedBody("associatedCompaniesFY1Count" -> "1", "associatedCompaniesFY2Count" -> "1")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
