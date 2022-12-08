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

package views.helpers

import connectors.sharedmodel._
import forms.AccountingPeriodForm
import play.api.i18n.Messages
import play.twirl.api.Html
import views.helpers.FullResultsPageHelper.{ nonTabCalculationResultsTable, taxDetailsWithAssociatedCompanies }
import views.helpers.PDFViewHelper.pdfFormulaAndNextHtml

import scala.collection.immutable.Seq

object PDFFileTemplateHelper {
  def pdfHowItsCalculated(
    calculatorResult: CalculatorResult,
    taxableProfit: Int,
    distributions: Int,
    associatedCompanies: Either[Int, (Int, Int)],
    config: Map[Int, FYConfig],
    pageCount: Int,
    accountingPeriodForm: AccountingPeriodForm
  )(implicit messages: Messages): Html = {
    calculatorResult match {
      case SingleResult(flatRate: FlatRate, _) =>
        Html(s"""
                |<div class="pdf-page">
                |        <div class="grid-row">
                |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
                |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
                |          ${nonTabCalculationResultsTable(
                 taxDetailsWithAssociatedCompanies(Seq(flatRate), associatedCompanies),
                 taxableProfit,
                 distributions,
                 config
               )}
                ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
                |        </div>
                |        <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
                |</div>
                |
                |""".stripMargin)
      case DualResult(flatRate1: FlatRate, flatRate2: FlatRate, _) =>
        Html(s"""
                |<div class="pdf-page">
                |        <div class="grid-row">
                |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
                |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
                |          ${nonTabCalculationResultsTable(
                 taxDetailsWithAssociatedCompanies(Seq(flatRate1, flatRate2), associatedCompanies),
                 taxableProfit,
                 distributions,
                 config
               )}
                ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
                |        </div>
                |        <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
                |</div>
                |""".stripMargin)
      case SingleResult(marginalRate: MarginalRate, _) =>
        Html(s"""
                |<div class="pdf-page">
                |        <div class="grid-row">
                |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
                |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
                |          ${nonTabCalculationResultsTable(
                 taxDetailsWithAssociatedCompanies(Seq(marginalRate), associatedCompanies),
                 taxableProfit,
                 distributions,
                 config
               )}
                ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
                |        </div>
                |   <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
                |</div>
                |""".stripMargin)
      case DualResult(flatRate: FlatRate, marginalRate: MarginalRate, _) =>
        Html(
          s"""
             |<div class="pdf-page">
             |        <div class="grid-row">
             |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
             |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
             |          ${nonTabCalculationResultsTable(
              taxDetailsWithAssociatedCompanies(Seq(flatRate, marginalRate), associatedCompanies),
              taxableProfit,
              distributions,
              config
            )}
             ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
             |        </div>
             |        <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
             |</div>
             |""".stripMargin
        )
      case DualResult(marginalRate: MarginalRate, flatRate: FlatRate, _) =>
        Html(s"""
                |<div class="pdf-page">
                |        <div class="grid-row">
                |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
                |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
                |          ${nonTabCalculationResultsTable(
                 taxDetailsWithAssociatedCompanies(Seq(marginalRate, flatRate), associatedCompanies),
                 taxableProfit,
                 distributions,
                 config
               )}
                ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
                |        </div>
                |        <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
                |</div>
                |""".stripMargin)
      case DualResult(marginalRate1: MarginalRate, marginalRate2: MarginalRate, _) =>
        Html(
          s"""
             |<div class="pdf-page">
             |      <div class="grid-row">
             |          <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
             |          <p class="govuk-body">${messages("fullResultsPage.marginalReliefForAccountingPeriod")}</p>
             |          ${nonTabCalculationResultsTable(
              associatedCompanies match {
                case Left(a)         => Seq(marginalRate1 -> a)
                case Right((a1, a2)) => Seq(marginalRate1 -> a1)
              },
              taxableProfit,
              distributions,
              config
            )}
             |      </div>
             |      <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
             |</div>
             |<div class="pdf-page">
             |          <div class="grid-row">
             |            ${nonTabCalculationResultsTable(
              associatedCompanies match {
                case Left(a)         => Seq(marginalRate2 -> a)
                case Right((a1, a2)) => Seq(marginalRate2 -> a2)
              },
              taxableProfit,
              distributions,
              config
            )}
             ${pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)}
             |      </div>
             |      <span class="govuk-body-s footer-page-no">${messages("pdf.page", "4", pageCount)}</span>
             |</div>
             |""".stripMargin
        )
    }
  }

  def numberOfPages(calculatorResult: CalculatorResult): Int = calculatorResult match {
    case DualResult(_: MarginalRate, _: MarginalRate, _) => 4
    case _                                               => 3
  }
}
