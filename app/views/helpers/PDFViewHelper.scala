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
import forms.DateUtils.DateOps
import forms.{ AccountingPeriodForm, PDFMetadataForm, TwoAssociatedCompaniesForm }
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.{ CurrencyUtils, DateUtils, PercentageUtils, ShowCalculatorDisclaimerUtils }
import views.helpers.FullResultsPageHelper.{ marginalReliefFormula, nonTabCalculationResultsTable, showMarginalReliefExplanation }
import views.helpers.ResultsPageHelper.{ displayBanner, displayCorporationTaxTable, displayEffectiveTaxTable, displayYourDetails }

import java.time.Instant
import scala.collection.immutable.Seq

object PDFViewHelper extends ViewHelper {

  def pdfTableHtml(
    calculatorResult: CalculatorResult,
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig],
    pdfMetadata: Option[PDFMetadataForm],
    accountingPeriodForm: AccountingPeriodForm,
    now: Instant,
    twoAssociatedCompanies: Option[TwoAssociatedCompaniesForm]
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
            now,
            twoAssociatedCompanies
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(Seq(flatRate), associatedCompanies, taxableProfit, distributions, config),
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
            now,
            twoAssociatedCompanies
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(
              Seq(flatRate1, flatRate2),
              associatedCompanies,
              taxableProfit,
              distributions,
              config
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
            now,
            twoAssociatedCompanies
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(Seq(m), associatedCompanies, taxableProfit, distributions, config),
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
            now,
            twoAssociatedCompanies
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
             ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(Seq(flatRate, m), associatedCompanies, taxableProfit, distributions, config),
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
            now,
            twoAssociatedCompanies
          )}
                ${pdfCorporationTaxHtml("3", calculatorResult)}
                ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(Seq(m, flatRate), associatedCompanies, taxableProfit, distributions, config),
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
            now,
            twoAssociatedCompanies
          )}
        ${pdfCorporationTaxHtml("4", calculatorResult)}
        ${pdfDetailedCalculationHtml(
            nonTabCalculationResultsTable(Seq(m1), associatedCompanies, taxableProfit, distributions, config),
            calculatorResult,
            accountingPeriodForm,
            "4"
          )}
        ${pdfDetailedCalculationHtmlWithoutHeader(
            nonTabCalculationResultsTable(Seq(m2), associatedCompanies, taxableProfit, distributions, config),
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
    associatedCompanies: Int,
    now: Instant,
    twoAssociatedCompanies: Option[TwoAssociatedCompaniesForm]
  )(implicit messages: Messages): Html =
    Html(
      s"""<div class="print-document">
         |              <div class="grid-row print-header">
         |                     <div class="govuk-grid-column-one-third">
         |                            <img class="print-header__hmrc-logo" src="data:image/jpeg;base64,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" alt="HM Revenue & Customs">
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
          twoAssociatedCompanies,
          ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(accountingPeriodForm.accountingPeriodEndDateOrDefault)
        )}
         |       </div>
         |       </div>
         |       </div>
         |<div class="govuk-grid-column-one-third govuk-!-padding-right-0">
         |<div class="about-results">
         |<h2 class="govuk-heading-s about-results-border">${messages("pdf.aboutThisResult")}</h3>
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
            |                  ${if (calculatorResult.totalMarginalRelief > 0) {
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
             calculatorResult.effectiveTaxRate
           )}</span>
            |                   ${if (calculatorResult.totalMarginalRelief > 0) {
             s"""<p class="govuk -body">${messages(
                 "resultsPage.reducedFromAfterMR",
                 PercentageUtils.format(calculatorResult.effectiveTaxRateBeforeMR)
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
        s"""
      |   $marginalReliefFormula
      |   ${views.helpers.FullResultsPageHelper.hr}"""
      } else s""}
       |<h3 class="govuk-heading-s">${messages("fullResultsPage.whatToDoNext")}</h3>
       |    <ul class="govuk-list govuk-list--bullet">
       |        <li>${messages("fullResultsPage.completeYourCorporationTaxReturn")}</li>
       |        <li>${messages(
        "fullResultsPage.payYourCorporationTaxBy"
      )} <b>${accountingPeriodForm.accountingPeriodEndDateOrDefault.plusMonths(9).plusDays(1).formatDateFull}</b>.</li>
       |    </ul>
       |""".stripMargin
  )
}
