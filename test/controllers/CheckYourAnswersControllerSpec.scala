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
import models.associatedCompanies.*
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ AssociatedCompanies, Distribution, DistributionsIncluded }
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AssociatedCompaniesParameterService
import viewmodels.checkAnswers.*
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView
import org.mockito.Mockito.when

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  private val requiredAnswers = emptyUserAnswers
    .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
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
      AssociatedCompaniesForm(AssociatedCompanies.No, None)
    )
    .get

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockParameterService: AssociatedCompaniesParameterService = mock[AssociatedCompaniesParameterService]

      val application = applicationBuilder(userAnswers = Some(requiredAnswers))
        .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
        .build()

      when(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(1)
        )
      ) thenReturn Future.successful(AskFull)
      implicit val msgs: Messages = messages(application)

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val askAssociatedCompaniesParam = AskFull
        val list = SummaryListViewModel(
          AccountingPeriodSummary.row(requiredAnswers) ++
            TaxableProfitSummary.row(requiredAnswers) ++
            DistributionSummary.row(requiredAnswers) ++
            AssociatedCompaniesSummary.row(requiredAnswers, askAssociatedCompaniesParam) ++
            TwoAssociatedCompaniesSummary.row(requiredAnswers, askAssociatedCompaniesParam)
        )

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view
          .render(list, routes.ResultsPageController.onPageLoad().url, request, messages(application))
          .toString
          .filterAndTrim
      }
    }

    "must return OK and the correct view for a GET, when accounting period end date is empty" in {

      val answers = requiredAnswers
        .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), None))
        .get

      val mockParameterService: AssociatedCompaniesParameterService =
        mock[AssociatedCompaniesParameterService]

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[AssociatedCompaniesParameterService].toInstance(mockParameterService))
        .build()

      when(
        mockParameterService.associatedCompaniesParameters(
          accountingPeriodStart = LocalDate.ofEpochDay(0),
          accountingPeriodEnd = LocalDate.ofEpochDay(0).plusYears(1).minusDays(1)
        )
      ) thenReturn Future.successful(AskFull)
      implicit val msgs: Messages = messages(application)

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val askAssociatedCompaniesParam = AskFull
        val list = SummaryListViewModel(
          AccountingPeriodSummary.row(answers) ++
            TaxableProfitSummary.row(answers) ++
            DistributionSummary.row(answers) ++
            AssociatedCompaniesSummary.row(answers, askAssociatedCompaniesParam) ++
            TwoAssociatedCompaniesSummary.row(answers, askAssociatedCompaniesParam)
        )

        status(result) mustEqual OK
        contentAsString(result).filterAndTrim mustEqual view
          .render(list, routes.ResultsPageController.onPageLoad().url, request, messages(application))
          .toString
          .filterAndTrim
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if required params is missing in user answers" in {

      val application = applicationBuilder(userAnswers =
        Some(
          emptyUserAnswers
            .set(AccountingPeriodPage, AccountingPeriodForm(LocalDate.ofEpochDay(0), Some(LocalDate.ofEpochDay(1))))
            .get
        )
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
