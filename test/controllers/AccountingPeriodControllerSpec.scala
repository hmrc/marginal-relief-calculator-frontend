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
import forms.{ AccountingPeriodForm, AccountingPeriodFormProvider, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ AssociatedCompanies, CheckMode, Distribution, DistributionsIncluded, NormalMode, UserAnswers }
import navigation.{ FakeNavigator, Navigator }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{ AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssociatedCompaniesParameterService
import repositories.SessionRepository
import uk.gov.hmrc.http.SessionKeys
import views.html.{ AccountingPeriodView, IrrelevantPeriodView }

import java.time.LocalDate
import scala.concurrent.Future

class AccountingPeriodControllerSpec extends SpecBase with MockitoSugar {
  private val epoch: LocalDate = LocalDate.parse("2022-04-02")
  val formProvider: AccountingPeriodFormProvider = new AccountingPeriodFormProvider()

  private def form(messages: Messages): Form[AccountingPeriodForm] = formProvider(using messages)

  lazy val completedUserAnswers: UserAnswers = UserAnswers(
    "test-session-id"
  ).set(
    AccountingPeriodPage,
    AccountingPeriodForm(
      LocalDate.parse("2023-03-23"),
      Some(LocalDate.parse("2024-02-23"))
    )
  ).get
    .set(
      TaxableProfitPage,
      70000
    )
    .get
    .set(
      DistributionPage,
      Distribution.Yes
    )
    .get
    .set(
      DistributionsIncludedPage,
      DistributionsIncludedForm(
        DistributionsIncluded.No,
        None
      )
    )
    .get
    .set(
      AssociatedCompaniesPage,
      AssociatedCompaniesForm(AssociatedCompanies.No, None)
    )
    .get

  def onwardRoute: Call = Call("GET", "/foo")
  val validAnswer: AccountingPeriodForm =
    AccountingPeriodForm(LocalDate.parse("2023-04-02"), Some(LocalDate.parse("2023-04-02").plusDays(1)))

  private lazy val accountingPeriodRoute = routes.AccountingPeriodController.onPageLoad(NormalMode).url
  private lazy val accountingPeriodRouteCheckMode = routes.AccountingPeriodController.onPageLoad(CheckMode).url

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, accountingPeriodRoute)

  def postRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, accountingPeriodRoute)
      .withFormUrlEncodedBody(
        "accountingPeriodStartDate.day"   -> validAnswer.accountingPeriodStartDate.getDayOfMonth.toString,
        "accountingPeriodStartDate.month" -> validAnswer.accountingPeriodStartDate.getMonthValue.toString,
        "accountingPeriodStartDate.year"  -> validAnswer.accountingPeriodStartDate.getYear.toString,
        "accountingPeriodEndDate.day"     -> validAnswer.accountingPeriodEndDateOrDefault.getDayOfMonth.toString,
        "accountingPeriodEndDate.month"   -> validAnswer.accountingPeriodEndDateOrDefault.getMonthValue.toString,
        "accountingPeriodEndDate.year"    -> validAnswer.accountingPeriodEndDateOrDefault.getYear.toString
      )

  "AccountingPeriod Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[AccountingPeriodView]

        status(result) `mustEqual` OK
        contentAsString(result).filterAndTrim `mustEqual` view
          .render(form(messages(application)), NormalMode, getRequest, messages(application))
          .toString
          .filterAndTrim
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(AccountingPeriodPage, validAnswer).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val view = application.injector.instanceOf[AccountingPeriodView]

        val result = route(application, getRequest).value

        status(result) `mustEqual` OK
        contentAsString(result).filterAndTrim `mustEqual` view(
          form(messages(application)).fill(validAnswer),
          NormalMode
        )(
          getRequest,
          messages(application)
        ).toString.filterAndTrim
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

      when(mockSessionRepository.set(any())) `thenReturn` Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.set(AccountingPeriodPage, validAnswer).get))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockParameterService, mockSessionRepository)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val result = route(application, postRequest).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value `mustEqual` onwardRoute.url
      }
    }

    "must return BadRequest 400 when form has invalid data" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val request = FakeRequest(POST, accountingPeriodRoute)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> "a",
            "accountingPeriodStartDate.month" -> "b",
            "accountingPeriodStartDate.year"  -> "c"
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val result = route(application, request).value

        status(result) `mustEqual` 400
        contentAsString(result) must include("The start date for the accounting period must be a real date")
      }
    }

    "must redirect to the next page when valid data is submitted, when user answers already exists" in {

      val application = applicationBuilder(userAnswers = Some(UserAnswers("test-session-id"))).build()
      val epochEndDate = epoch.plusYears(1).minusDays(1)
      running(application) {

        val request = FakeRequest(POST, accountingPeriodRoute)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> epoch.getDayOfMonth.toString,
            "accountingPeriodStartDate.month" -> epoch.getMonth.getValue.toString,
            "accountingPeriodStartDate.year"  -> epoch.getYear.toString,
            "accountingPeriodEndDate.day"     -> epochEndDate.getDayOfMonth.toString,
            "accountingPeriodEndDate.month"   -> epochEndDate.getMonth.getValue.toString,
            "accountingPeriodEndDate.year"    -> epochEndDate.getYear.toString
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val sessionRepository = application.injector.instanceOf[SessionRepository]
        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.TaxableProfitController.onPageLoad(NormalMode).url))
        sessionRepository
          .get("test-session-id")
          .futureValue
          .get
          .data
          .value(AccountingPeriodPage.toString)
          .as[AccountingPeriodForm] must be(
          AccountingPeriodForm(
            accountingPeriodStartDate = epoch,
            accountingPeriodEndDate = Some(epochEndDate)
          )
        )
      }
    }

    "must redirect to the next page when valid data is submitted, when user answers don't already exist" in {
      val application = applicationBuilder(userAnswers = None).build()
      val epochEndDate = epoch.plusYears(1).minusDays(1)
      running(application) {

        val request = FakeRequest(POST, accountingPeriodRoute)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> epoch.getDayOfMonth.toString,
            "accountingPeriodStartDate.month" -> epoch.getMonth.getValue.toString,
            "accountingPeriodStartDate.year"  -> epoch.getYear.toString,
            "accountingPeriodEndDate.day"     -> epochEndDate.getDayOfMonth.toString,
            "accountingPeriodEndDate.month"   -> epochEndDate.getMonth.getValue.toString,
            "accountingPeriodEndDate.year"    -> epochEndDate.getYear.toString
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val sessionRepository = application.injector.instanceOf[SessionRepository]
        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.TaxableProfitController.onPageLoad(NormalMode).url))
        sessionRepository
          .get("test-session-id")
          .futureValue
          .get
          .data
          .value(AccountingPeriodPage.toString)
          .as[AccountingPeriodForm] must be(
          AccountingPeriodForm(
            accountingPeriodStartDate = epoch,
            accountingPeriodEndDate = Some(epochEndDate)
          )
        )
      }
    }

    "must redirect to change page if user changed existing accounting period dates" in {
      val application = applicationBuilder(userAnswers = Some(completedUserAnswers)).build()

      running(application) {
        val form = completedUserAnswers.get(AccountingPeriodPage).get
        val sDate = form.accountingPeriodStartDate
        val eDate = form.accountingPeriodEndDateOrDefault.minusMonths(1)
        val request = FakeRequest(POST, accountingPeriodRouteCheckMode)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> sDate.getDayOfMonth.toString,
            "accountingPeriodStartDate.month" -> sDate.getMonth.getValue.toString,
            "accountingPeriodStartDate.year"  -> sDate.getYear.toString,
            "accountingPeriodEndDate.day"     -> eDate.getDayOfMonth.toString,
            "accountingPeriodEndDate.month"   -> eDate.getMonth.getValue.toString,
            "accountingPeriodEndDate.year"    -> eDate.getYear.toString
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.CheckYourAnswersController.onPageLoad().url))
      }
    }

    "must redirect to change page if user did not change existing accounting period dates" in {
      val application = applicationBuilder(userAnswers = Some(completedUserAnswers)).build()

      running(application) {
        val form = completedUserAnswers.get(AccountingPeriodPage).get
        val sDate = form.accountingPeriodStartDate
        val eDate = form.accountingPeriodEndDateOrDefault
        val request = FakeRequest(POST, accountingPeriodRouteCheckMode)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> sDate.getDayOfMonth.toString,
            "accountingPeriodStartDate.month" -> sDate.getMonth.getValue.toString,
            "accountingPeriodStartDate.year"  -> sDate.getYear.toString,
            "accountingPeriodEndDate.day"     -> eDate.getDayOfMonth.toString,
            "accountingPeriodEndDate.month"   -> eDate.getMonth.getValue.toString,
            "accountingPeriodEndDate.year"    -> eDate.getYear.toString
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.CheckYourAnswersController.onPageLoad().url))
      }
    }

    "must NOT redirect to Check your answers page if form is prefilled, not changed and in normal mode" in {
      val application = applicationBuilder(userAnswers = Some(completedUserAnswers)).build()

      running(application) {
        val form = completedUserAnswers.get(AccountingPeriodPage).get
        val sDate = form.accountingPeriodStartDate
        val eDate = form.accountingPeriodEndDateOrDefault
        val request = FakeRequest(POST, accountingPeriodRoute)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> sDate.getDayOfMonth.toString,
            "accountingPeriodStartDate.month" -> sDate.getMonth.getValue.toString,
            "accountingPeriodStartDate.year"  -> sDate.getYear.toString,
            "accountingPeriodEndDate.day"     -> eDate.getDayOfMonth.toString,
            "accountingPeriodEndDate.month"   -> eDate.getMonth.getValue.toString,
            "accountingPeriodEndDate.year"    -> eDate.getYear.toString
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")
        val sessionRepository = application.injector.instanceOf[SessionRepository]

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must not be Some(routes.CheckYourAnswersController.onPageLoad().url)
        sessionRepository
          .get("test-session-id")
          .futureValue
          .get
          .data must be(
          completedUserAnswers.data
        )
      }
    }

    "must be able to open irrelevant period page" in {
      val application = applicationBuilder(None).build()
      running(application) {
        val view = application.injector.instanceOf[IrrelevantPeriodView]

        val request = FakeRequest(routes.AccountingPeriodController.irrelevantPeriodPage(NormalMode))

        val result = route(application, request).value

        status(result) `mustEqual` OK

        contentAsString(result).filterAndTrim `mustEqual` view
          .render(
            NormalMode,
            request,
            messages(application)
          )
          .toString
          .filterAndTrim
      }
    }

    "must redirect to irrelevant period page if start date is before 2nd of April 2022" in {
      val application = applicationBuilder(userAnswers = Some(completedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, accountingPeriodRouteCheckMode)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> "1",
            "accountingPeriodStartDate.month" -> "4",
            "accountingPeriodStartDate.year"  -> "2022"
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val result = route(application, request).value
        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.AccountingPeriodController.irrelevantPeriodPage(CheckMode).url))
      }
    }

    "must redirect to irrelevant period page if end date is before 1st of April 2023" in {
      val application = applicationBuilder(userAnswers = Some(completedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, accountingPeriodRouteCheckMode)
          .withFormUrlEncodedBody(
            "accountingPeriodStartDate.day"   -> "2",
            "accountingPeriodStartDate.month" -> "4",
            "accountingPeriodStartDate.year"  -> "2022",
            "accountingPeriodEndDate.day"     -> "31",
            "accountingPeriodEndDate.month"   -> "3",
            "accountingPeriodEndDate.year"    -> "2023"
          )
          .withSession(SessionKeys.sessionId -> "test-session-id")

        val result = route(application, request).value
        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result) must be(Some(routes.AccountingPeriodController.irrelevantPeriodPage(CheckMode).url))
      }
    }
  }
}
