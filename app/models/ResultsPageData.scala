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

package models

import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel.CalculatorResult
import forms.AccountingPeriodForm
import models.requests.DataRequest
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionsIncludedPage, TaxableProfitPage }
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

object ResultsPageData {
  def apply(
    marginalReliefCalculatorConnector: MarginalReliefCalculatorConnector
  )(implicit request: DataRequest[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ResultsPageData]] = {
    val maybeAccountingPeriodForm = request.userAnswers.get(AccountingPeriodPage)
    val maybeTaxableProfit = request.userAnswers.get(TaxableProfitPage)
    val maybeAssociatedCompanies = request.userAnswers.get(AssociatedCompaniesPage)
    val maybeDistributionsIncludedForm = request.userAnswers.get(DistributionsIncludedPage)

    (maybeAccountingPeriodForm, maybeTaxableProfit) match {
      case (Some(accountingPeriodForm), Some(taxableProfit)) =>
        marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodForm.accountingPeriodStartDate,
            accountingPeriodForm.accountingPeriodEndDate.get,
            taxableProfit.toDouble,
            maybeDistributionsIncludedForm.flatMap(
              _.distributionsIncludedAmount.map(_.toDouble)
            ),
            maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount),
            maybeAssociatedCompanies.flatMap(_.associatedCompaniesFY1Count),
            maybeAssociatedCompanies.flatMap(_.associatedCompaniesFY2Count)
          )
          .map(calculatorResult =>
            Some(
              ResultsPageData(
                accountingPeriodForm,
                taxableProfit,
                calculatorResult,
                maybeDistributionsIncludedForm.flatMap(_.distributionsIncludedAmount).getOrElse(0),
                maybeAssociatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0)
              )
            )
          )
      case _ => Future.successful(None)
    }

  }
}

case class ResultsPageData(
  accountingPeriodForm: AccountingPeriodForm,
  taxableProfit: Int,
  calculatorResult: CalculatorResult,
  distributionsIncludedAmount: Int,
  associatedCompaniesCount: Int
)
