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

package views.helpers

import models.FYConfig
import models.calculator.{ CalculatorResult, DualResult, FlatRate, MarginalRate, SingleResult }
import forms.DateUtils.DateOps
import forms.{ AccountingPeriodForm, DateUtils, PDFMetadataForm }
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.{ CurrencyUtils, PercentageUtils, ShowCalculatorDisclaimerUtils }
import views.helpers.FullResultsPageHelper.{ marginalReliefFormula, nonTabCalculationResultsTable, showMarginalReliefExplanation, taxDetailsWithAssociatedCompanies }
import views.helpers.ResultsPageHelper.{ displayBanner, displayCorporationTaxTable, displayEffectiveTaxTable, displayYourDetails }

import java.time.Instant
import scala.collection.immutable.Seq

object PDFViewHelper extends ViewHelper {

  def pdfTableHtml(
    calculatorResult: CalculatorResult,
    associatedCompanies: Either[Int, (Int, Int)],
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig],
    pdfMetadata: Option[PDFMetadataForm],
    accountingPeriodForm: AccountingPeriodForm,
    now: Instant
  )(implicit messages: Messages): Html =
    calculatorResult match {
      case SingleResult(flatRate: FlatRate, _) =>
        Html(s"""
        ${pdfHeaderHtml(
            "3",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
          ${pdfCorporationTaxHtml("3", calculatorResult)}
          ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              taxDetailsWithAssociatedCompanies(Seq(flatRate), associatedCompanies),
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "3"
          )}
             """)
      case DualResult(flatRate1: FlatRate, flatRate2: FlatRate, _) =>
        Html(s"""
              ${pdfHeaderHtml(
            "3",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              taxDetailsWithAssociatedCompanies(Seq(flatRate1, flatRate2), associatedCompanies),
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "3"
          )}
             """)
      case SingleResult(m: MarginalRate, _) =>
        Html(s"""
              ${pdfHeaderHtml(
            "3",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              taxDetailsWithAssociatedCompanies(Seq(m), associatedCompanies),
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "3"
          )}
             """)
      case DualResult(flatRate: FlatRate, m: MarginalRate, _) =>
        Html(s"""
              ${pdfHeaderHtml(
            "3",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              taxDetailsWithAssociatedCompanies(Seq(flatRate, m), associatedCompanies),
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "3"
          )}
             """)

      case DualResult(m: MarginalRate, flatRate: FlatRate, _) =>
        Html(s"""
                ${pdfHeaderHtml(
            "3",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
                ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              taxDetailsWithAssociatedCompanies(Seq(m, flatRate), associatedCompanies),
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "3"
          )}""")

      case DualResult(m1: MarginalRate, m2: MarginalRate, _) =>
        Html(s"""
        ${pdfHeaderHtml(
            "4",
            pdfMetadata,
            calculatorResult,
            accountingPeriodForm,
            taxableProfit,
            distributions,
            associatedCompanies,
            now
          )}
        ${pdfCorporationTaxHtml("4", calculatorResult)}
        ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              calculatorResult,
              associatedCompanies match {
                case Left(a)         => Seq(m1 -> a)
                case Right((a1, a2)) => Seq(m1 -> a1)
              },
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "4"
          )}
        ${pdfDetailedCalculationHtmlWithoutHeader(
            nonTabCalculationResultsTable(
              calculatorResult,
              associatedCompanies match {
                case Left(a)         => Seq(m2 -> a)
                case Right((a1, a2)) => Seq(m2 -> a2)
              },
              taxableProfit,
              distributions,
              config,
              isPDF = true
            ),
            calculatorResult,
            accountingPeriodForm,
            "4"
          )}""")
    }

  def pdfDetailedCalculationHtml(
    resultsTable: Html,
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    pageCount: String
  )(implicit
    messages: Messages
  ): Html =
    Html(s"""<div class="print-document">
            |            <div class="grid-row">
            |            <h2 class="govuk-heading-l">${messages("fullResultsPage.howItsCalculated")}</h2>
            |             $resultsTable
            |            ${if (pageCount == "3") {
             pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)
           } else s""}
            |           </div>
            |         <span class="govuk-body-s footer-page-no">${messages("pdf.page", "3", pageCount)}</span>
            |        </div>""".stripMargin)

  def pdfDetailedCalculationHtmlWithoutHeader(
    resultsTable: Html,
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    pageCount: String
  )(implicit
    messages: Messages
  ): Html =
    Html(s"""<div class="print-document">
            |          <div class="grid-row">
            |            $resultsTable
            |            ${if (pageCount == "4") {
             pdfFormulaAndNextHtml(accountingPeriodForm, calculatorResult)
           } else s""}
            |          </div>
            |          <span class="govuk-body-s footer-page-no">${messages("pdf.page", "4", pageCount)}</span>
            |        </div>""".stripMargin)

  def pdfHeaderHtml(
    pageCount: String,
    pdfMetadata: Option[PDFMetadataForm],
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    taxableProfit: Int,
    distributions: Int,
    associatedCompanies: Either[Int, (Int, Int)],
    now: Instant
  )(implicit messages: Messages): Html =
    Html(
      s"""<div class="print-document">
         |              <div class="grid-row print-header">
         |                     <div class="govuk-grid-column-one-third">
         |                            <img class="print-header__hmrc-logo" src="data:image/png;base64,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" alt="HM Revenue & Customs">
         |                            <span class="govuk-heading-m">${messages("pdf.logoTitle")}</span>
         |                        </div>
         |                        <div class="govuk-grid-column-two-thirds">
         |                            <h2 class="govuk-heading-m print-header__heading">
         |                                ${messages("pdf.previewTitle")}
         |                            </h2>
         |                        </div>
         |                    </div>
         | ${if (pdfMetadata.nonEmpty) {
          pdfUtrCompanyName(pdfMetadata.map(_.companyName).get, pdfMetadata.map(_.utr.map(_.toString)).get)
        } else ""}
         |       <div class="grid-row print-banner">${replaceBannerHtml(displayBanner(calculatorResult).html)}</div>
         |       <div class="grid-row">
         |       <div class="govuk-grid-column-full">
         |       <div class="grid-row">
         |       <div class="govuk-grid-column-two-thirds govuk-!-padding-left-0">
         |       <div class="grid-row">
         |       <div class="govuk-grid-column-full">
         |         ${displayYourDetails(
          calculatorResult,
          accountingPeriodForm,
          taxableProfit,
          distributions,
          associatedCompanies,
          false,
          ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(accountingPeriodForm.accountingPeriodEndDateOrDefault)
        )}
         |       </div>
         |       </div>
         |       </div>
         |<div class="govuk-grid-column-one-third govuk-!-padding-right-0">
         |<div class="about-results">
         |<h2 class="govuk-heading-s about-results-border">${messages("pdf.aboutThisResult")}</h2>
         | <h3 class="govuk-heading-xs">${messages("pdf.dataOfResult")}</h3>
         | <p class="govuk-body about-results-border">${Html(DateUtils.formatInstantUTC(now))}</p>
         | <h3 class="govuk-heading-xs">${messages("pdf.legalDeclarationTitle")}</h3>
         |<p class="govuk-body">${messages("pdf.legalDeclaration")}</p>
         |</div>
         |</div>
         |</div>
         |</div>
         |</div>
         | <span class="govuk-body-s footer-page-no">${messages("pdf.page", "1", pageCount)}</span>
         | </div>""".stripMargin
    )

  def pdfUtrCompanyName(companyName: Option[String], utr: Option[String])(implicit messages: Messages): Html =
    Html(s"""
            | ${if (companyName.nonEmpty) {
             s"""<div class="grid-row">
          <h2 class="govuk-heading-s govuk-!-static-margin-bottom-1">${messages("pdf.companyName")}</h2>
          <p class="govuk-body">${companyName.get}</p>
          </div>"""
           } else s""}
          ${if (utr.nonEmpty) {
             s"""<div class="grid-row">
            <h2 class="govuk-heading-s govuk-!-static-margin-bottom-1">${messages("pdf.utr")}</h2>
            <p class="govuk-body">${utr.get}</p>
          </div>"""
           } else s""}
            |""".stripMargin)

  def pdfCorporationTaxHtml(pageCount: String, calculatorResult: CalculatorResult)(implicit
    messages: Messages
  ): Html =
    Html(s"""<div class="print-document">
            |            <div class="grid-row">
            |                  <h2 class="govuk-heading-m" style="margin-bottom: 7px;">${messages(
             "resultsPage.corporationTaxLiability"
           )}</h2>
            |                  <span class="govuk-heading-l" style="margin-bottom: 4px;">${CurrencyUtils.format(
             calculatorResult.totalCorporationTax
           )}</span>
            |                  ${if (calculatorResult.taxDetails.isInstanceOf[MarginalRate]) {
             s"""<p class="govuk -body">${messages(
                 "resultsPage.corporationTaxReducedFrom",
                 CurrencyUtils.format(calculatorResult.totalCorporationTaxBeforeMR),
                 CurrencyUtils.format(calculatorResult.totalMarginalRelief)
               )}</p>"""
           } else s""}
            |  <div class="app-table" role="region" aria-label="${messages(
             "resultsPage.corporationTaxTable.hidden"
           )}" tabindex="0">
            |                  ${displayCorporationTaxTable(calculatorResult)}
            |                  </div>
            |               </div>
            |               <div class="grid-row">
            |                   <h2 class="govuk-heading-m" style="margin-bottom: 7px;">${messages(
             "resultsPage.effectiveTaxRate"
           )}</h2>
            |                   <span class="govuk-heading-l" style="margin-bottom: 4px;">${PercentageUtils.format(
             calculatorResult.effectiveTaxRate.doubleValue
           )}</span>
            |                   ${if (calculatorResult.taxDetails.isInstanceOf[MarginalRate]) {
             s"""<p class="govuk -body">${messages(
                 "resultsPage.reducedFromAfterMR",
                 PercentageUtils.format(
                   calculatorResult.taxDetails.asInstanceOf[MarginalRate].taxRateBeforeMR.doubleValue
                 )
               )}</p>"""
           } else s""}
            |  <div class="app-table" role="region" aria-label="${messages(
             "resultsPage.effectiveTaxRateTable.hidden"
           )}" tabindex="0">
            |                   ${displayEffectiveTaxTable(calculatorResult)}
            |                   </div>
            |               </div>
            |               <span class="govuk-body-s footer-page-no">${messages("pdf.page", "2", pageCount)}</span>
            |           </div>""".stripMargin)

  def replaceBannerHtml(bannerHtml: Html): Html =
    Html(
      bannerHtml
        .toString()
        .replaceAll("[\n\r]", "")
        .replace(
          "<h1",
          "<h2"
        )
        .replace(
          "</h1>",
          "</h2>"
        )
    )

  def pdfFormulaAndNextHtml(accountingPeriodForm: AccountingPeriodForm, calculatorResult: CalculatorResult)(implicit
    messages: Messages
  ): Html = Html(
    s"""
      ${if (showMarginalReliefExplanation(calculatorResult)) {
        s"""$marginalReliefFormula${views.helpers.FullResultsPageHelper.hr}"""
      } else ""}
       |<h3 class="govuk-heading-s">${messages("fullResultsPage.whatToDoNext")}</h3>
       |    <ul class="govuk-list govuk-list--bullet">
       |        <li>${messages("fullResultsPage.completeYourCorporationTaxReturn")}</li>
       |        <li>${messages(
        "fullResultsPage.payYourCorporationTaxBy"
      )} <b>${accountingPeriodForm.accountingPeriodEndDateOrDefault
        .plusMonths(9)
        .plusDays(1)
        .govDisplayFormat}</b>.</li>
       |    </ul>
       |""".stripMargin
  )
}
