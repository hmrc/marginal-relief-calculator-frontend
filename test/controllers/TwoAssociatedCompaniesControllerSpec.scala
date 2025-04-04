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
import models.associatedCompanies.{ AskBothParts, DontAsk, Period }
import forms.DateUtils.financialYear
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, TwoAssociatedCompaniesForm, TwoAssociatedCompaniesFormProvider }
import models.{ AssociatedCompanies, Distribution, NormalMode, UserAnswers }
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, TaxableProfitPage, TwoAssociatedCompaniesPage }
import play.api.data.FormError
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import views.html.TwoAssociatedCompaniesView

import java.time.LocalDate
import scala.concurrent.Future

class TwoAssociatedCompaniesControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val formProvider = new TwoAssociatedCompaniesFormProvider()
  private val form = formProvider(1, 2)

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
    .get
    .set(TaxableProfitPage, 1)
    .get
    .set(DistributionPage, Distribution.No)
    .get
    .set(AssociatedCompaniesPage, AssociatedCompaniesForm(AssociatedCompanies.Yes, None))
    .get

  private lazy val twoAssociatedCompaniesRoute = routes.TwoAssociatedCompaniesController.onPageLoad(NormalMode).url

  "TwoAssociatedCompanies Controller" - {

    "GET page" - {

      "must return OK and the correct view for a GET" in {

        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1)))
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(userAnswers = Some(requiredAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
        ) thenReturn Future.successful(askParameter)

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

      "must return OK and the correct view for a GET when accounting end date is empty" in {

        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), None)

        val answers = requiredAnswers
          .set(AccountingPeriodPage, accountingPeriodForm)
          .get

        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusYears(1).minusDays(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusYears(1).minusDays(1))
        )
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(0).plusYears(1).minusDays(1)
          )
        ) thenReturn Future.successful(askParameter)

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

        val mockParameterService: AssociatedCompaniesParameterService =
          mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()

        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
        ) thenReturn Future.successful(askParameter)

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

      "must populate the view correctly on a GET when the question has previously been answered, and accounting end date is empty" in {

        val answers = requiredAnswers
          .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), None))
          .get

        val valid = TwoAssociatedCompaniesForm(Some(1), Some(1))
        val userAnswers = answers.set(TwoAssociatedCompaniesPage, valid).success.value
        val accountingPeriodForm = AccountingPeriodForm(LocalDate.ofEpochDay(0), None)
        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(0).plusYears(1).minusDays(1))
        )

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
          .build()
        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(0).plusYears(1).minusDays(1)
          )
        ) thenReturn Future.successful(askParameter)

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

      "must return SEE_OTHER and redirect to journey recovery page if distributions page available" in {

        val askParameter = AskBothParts(
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1)),
          Period(LocalDate.ofEpochDay(0), LocalDate.ofEpochDay(1))
        )
        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers.set(DistributionPage, Distribution.Yes).get))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
        ) thenReturn Future.successful(askParameter)

        running(application) {
          val request = FakeRequest(GET, twoAssociatedCompaniesRoute)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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

      "must throw unsupported error if associated parameter is not AskBothParts" in {

        val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

        val application =
          applicationBuilder(userAnswers = Some(requiredAnswers.set(DistributionPage, Distribution.No).get))
            .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
            .build()

        when(
          mockParameterService.associatedCompaniesParameters(
            accountingPeriodStart = LocalDate.ofEpochDay(0),
            accountingPeriodEnd = LocalDate.ofEpochDay(1)
          )
        ) thenReturn Future.successful(DontAsk)

        running(application) {
          val request = FakeRequest(GET, twoAssociatedCompaniesRoute)
          assert(route(application, request).value.failed.futureValue.isInstanceOf[UnsupportedOperationException])
        }
      }
    }

    "POST" - {

      "must redirect to check your answers page, when valid data is submitted" in {

        val epoch = LocalDate.ofEpochDay(0)
        val table = Table(
          ("scenario", "accountingPeriodForm"),
          ("Form with set end-date", AccountingPeriodForm(epoch, Some(epoch.plusYears(1).minusDays(1)))),
          ("Form without end-date(default)", AccountingPeriodForm(epoch, None))
        )

        forAll(table) { (_, accountingPeriodForm) =>
          val askParameter = AskBothParts(
            Period(
              accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodForm.accountingPeriodStartDate.withMonth(3).withDayOfMonth(31)
            ),
            Period(
              accountingPeriodForm.accountingPeriodStartDate.withMonth(4).withDayOfMonth(1),
              accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          )

          val mockSessionRepository = mock[SessionRepository]
          val mockParameterService: AssociatedCompaniesParameterService =
            mock[AssociatedCompaniesParameterService]

          when(
            mockParameterService.associatedCompaniesParameters(
              accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          ) thenReturn Future.successful(askParameter)
          when(mockSessionRepository.set(ArgumentMatchers.any(classOf[UserAnswers]))) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(requiredAnswers.set(AccountingPeriodPage, accountingPeriodForm).get))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AssociatedCompaniesParameterService].toInstance(mockParameterService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, twoAssociatedCompaniesRoute)
                .withFormUrlEncodedBody("associatedCompaniesFY1Count" -> "1", "associatedCompaniesFY2Count" -> "1")

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad().url
          }
        }
      }

      "must return a Bad Request and errors when form data binding fails" in {

        val epoch = LocalDate.ofEpochDay(0)
        val table = Table(
          ("scenario", "accountingPeriodForm"),
          ("Form with set end-date", AccountingPeriodForm(epoch, Some(epoch.plusYears(1).minusDays(1)))),
          ("Form without end-date(default)", AccountingPeriodForm(epoch, None))
        )

        forAll(table) { (_, accountingPeriodForm) =>
          val form = formProvider(
            financialYear(accountingPeriodForm.accountingPeriodStartDate),
            financialYear(accountingPeriodForm.accountingPeriodEndDateOrDefault)
          )
          val askParameter = AskBothParts(
            Period(
              accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodForm.accountingPeriodStartDate.withMonth(3).withDayOfMonth(31)
            ),
            Period(
              accountingPeriodForm.accountingPeriodStartDate.withMonth(4).withDayOfMonth(1),
              accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          )

          val mockSessionRepository = mock[SessionRepository]
          val mockParameterService: AssociatedCompaniesParameterService =
            mock[AssociatedCompaniesParameterService]

          when(
            mockParameterService.associatedCompaniesParameters(
              accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          ) thenReturn Future.successful(askParameter)
          when(mockSessionRepository.set(ArgumentMatchers.any(classOf[UserAnswers]))) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(requiredAnswers.set(AccountingPeriodPage, accountingPeriodForm).get))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AssociatedCompaniesParameterService].toInstance(mockParameterService)
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
            contentAsString(result).filterAndTrim mustEqual view
              .render(boundForm, accountingPeriodForm, askParameter, NormalMode, request, messages(application))
              .toString
              .filterAndTrim
          }
        }
      }

      "must return a Bad Request when form data fails validation" in {

        val epoch = LocalDate.ofEpochDay(0)
        val table = Table(
          ("scenario", "accountingPeriodForm"),
          ("Form with set end-date", AccountingPeriodForm(epoch, Some(epoch.plusYears(1).minusDays(1)))),
          ("Form without end-date(default)", AccountingPeriodForm(epoch, None))
        )

        forAll(table) { (_, accountingPeriodForm) =>
          val askParameter = AskBothParts(
            Period(
              accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodForm.accountingPeriodStartDate.withMonth(3).withDayOfMonth(31)
            ),
            Period(
              accountingPeriodForm.accountingPeriodStartDate.withMonth(4).withDayOfMonth(1),
              accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          )

          val mockSessionRepository = mock[SessionRepository]
          val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

          when(
            mockParameterService.associatedCompaniesParameters(
              accountingPeriodStart = accountingPeriodForm.accountingPeriodStartDate,
              accountingPeriodEnd = accountingPeriodForm.accountingPeriodEndDateOrDefault
            )
          ) thenReturn Future.successful(askParameter)
          when(mockSessionRepository.set(ArgumentMatchers.any(classOf[UserAnswers]))) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(requiredAnswers.set(AccountingPeriodPage, accountingPeriodForm).get))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[AssociatedCompaniesParameterService].toInstance(mockParameterService)
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
