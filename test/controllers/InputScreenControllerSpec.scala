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
import forms.InputScreenForm
import models.{NormalMode, UserAnswers}
import org.mockito.MockitoSugar
import pages.InputScreenPage
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.Helpers.{POST, status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import repositories.SessionRepository
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate

class InputScreenControllerSpec extends SpecBase with MockitoSugar {

  private val epoch: LocalDate = LocalDate.ofEpochDay(0)


  "InputScreenController" - {
    "onSubmit" - {
      "when accountingPeriodEndDate is missing, should default to (accountingPeriodStartDate + 1yr - 1d)" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            method = POST,
            uri = routes.InputScreenController.onSubmit(NormalMode).url,
            headers = FakeHeaders(Seq.empty),
            body = AnyContentAsFormUrlEncoded(
              Map(
                "accountingPeriodStartDate.day"   -> Seq(epoch.getDayOfMonth.toString),
                "accountingPeriodStartDate.month" -> Seq(epoch.getMonth.getValue.toString),
                "accountingPeriodStartDate.year"  -> Seq(epoch.getYear.toString),
                "profit"                          -> Seq("11"),
                "distribution"                    -> Seq("22"),
                "associatedCompanies"             -> Seq("33")
              )
            )
          ).withSession(SessionKeys.sessionId -> "test-session-id")

          val sessionRepository = application.injector.instanceOf[SessionRepository]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) must be(Some("/marginal-relief-calculator-frontend/results-page"))
          sessionRepository
            .get("test-session-id")
            .futureValue
            .get
            .data
            .value(InputScreenPage.toString)
            .as[InputScreenForm] must be(
            InputScreenForm(
              accountingPeriodStartDate = epoch,
              accountingPeriodEndDate = Some(epoch.plusYears(1).minusDays(1)),
              profit = 11,
              distribution = 22,
              associatedCompanies = 33
            )
          )
        }
      }
      "when accountingPeriodStartDate year is leap year than accountingPeriodEndDate should be + 366 days" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            method = POST,
            uri = routes.InputScreenController.onSubmit(NormalMode).url,
            headers = FakeHeaders(Seq.empty),
            body = AnyContentAsFormUrlEncoded(
              Map(
                "accountingPeriodStartDate.day"   -> Seq("01"),
                "accountingPeriodStartDate.month" -> Seq("02"),
                "accountingPeriodStartDate.year"  -> Seq("2020"),
                "profit"                          -> Seq("11"),
                "distribution"                    -> Seq("22"),
                "associatedCompanies"             -> Seq("33")
              )
            )
          ).withSession(SessionKeys.sessionId -> "test-session-id")

          val sessionRepository = application.injector.instanceOf[SessionRepository]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) must be(Some("/marginal-relief-calculator-frontend/results-page"))
          sessionRepository
            .get("test-session-id")
            .futureValue
            .get
            .data
            .value(InputScreenPage.toString)
            .as[InputScreenForm] must be(
            InputScreenForm(
              accountingPeriodStartDate = LocalDate.parse("2020-02-01"),
              accountingPeriodEndDate = Some(LocalDate.parse("2020-02-01").plusYears(1).minusDays(1)),
              profit = 11,
              distribution = 22,
              associatedCompanies = 33
            )
          )
        }
      }
      "When form is sent with invalid data should be 404" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            method = POST,
            uri = routes.InputScreenController.onSubmit(NormalMode).url,
            headers = FakeHeaders(Seq.empty),
            body = AnyContentAsFormUrlEncoded(
              Map(
                "accountingPeriodStartDate.day"   -> Seq("a"),
                "accountingPeriodStartDate.month" -> Seq("b"),
                "accountingPeriodStartDate.year"  -> Seq("c"),
                "profit"                          -> Seq("1"),
                "distribution"                    -> Seq("1"),
                "associatedCompanies"             -> Seq("1")
              )
            )
          )

          val result = route(application, request).value

          status(result) mustEqual 400
          contentAsString(result) must include("Enter a real Accounting Period Start date");
        }
      }
      "when user already in database" in {

        val application = applicationBuilder(userAnswers = Some(UserAnswers("test-session-id"))).build()

        running(application) {
          val request = FakeRequest(
            method = POST,
            uri = routes.InputScreenController.onSubmit(NormalMode).url,
            headers = FakeHeaders(Seq.empty),
            body = AnyContentAsFormUrlEncoded(
              Map(
                "accountingPeriodStartDate.day"   -> Seq(epoch.getDayOfMonth.toString),
                "accountingPeriodStartDate.month" -> Seq(epoch.getMonth.getValue.toString),
                "accountingPeriodStartDate.year"  -> Seq(epoch.getYear.toString),
                "profit"                          -> Seq("11"),
                "distribution"                    -> Seq("22"),
                "associatedCompanies"             -> Seq("33")
              )
            )
          ).withSession(SessionKeys.sessionId -> "test-session-id")

          val sessionRepository = application.injector.instanceOf[SessionRepository]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) must be(Some("/marginal-relief-calculator-frontend/results-page"))
          sessionRepository
            .get("test-session-id")
            .futureValue
            .get
            .data
            .value(InputScreenPage.toString)
            .as[InputScreenForm] must be(
            InputScreenForm(
              accountingPeriodStartDate = epoch,
              accountingPeriodEndDate = Some(epoch.plusYears(1).minusDays(1)),
              profit = 11,
              distribution = 22,
              associatedCompanies = 33
            )
          )
        }
      }
    }
  }
}
