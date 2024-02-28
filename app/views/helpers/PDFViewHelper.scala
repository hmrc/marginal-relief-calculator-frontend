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
         |                            <img class="print-header__hmrc-logo" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAQAAAAAeCAYAAADD2cA5AAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAA5mVYSWZNTQAqAAAACAAGARIAAwAAAAEAAQAAARoABQAAAAEAAABWARsABQAAAAEAAABeATEAAgAAACEAAABmATIAAgAAABQAAACIh2kABAAAAAEAAACcAAAAAAAAASwAAAABAAABLAAAAAFBZG9iZSBQaG90b3Nob3AgMjQuNyAoTWFjaW50b3NoKQAAMjAyMzowODoyOSAwNzo1MjowMgAABJAEAAIAAAAUAAAA0qABAAMAAAABAAEAAKACAAQAAAABAAABAKADAAQAAAABAAAAHgAAAAAyMDIzOjA4OjE1IDA4OjU5OjEzAIGzN38AAAAJcEhZcwAALiMAAC4jAXilP3YAAAd2aVRYdFhNTDpjb20uYWRvYmUueG1wAAAAAAA8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJYTVAgQ29yZSA2LjAuMCI+CiAgIDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+CiAgICAgIDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiCiAgICAgICAgICAgIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIKICAgICAgICAgICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgICAgICAgICB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIKICAgICAgICAgICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgICAgICAgICAgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iCiAgICAgICAgICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyI+CiAgICAgICAgIDxkYzpmb3JtYXQ+aW1hZ2UvcG5nPC9kYzpmb3JtYXQ+CiAgICAgICAgIDx4bXA6TW9kaWZ5RGF0ZT4yMDIzLTA4LTI5VDA3OjUyOjAyKzAxOjAwPC94bXA6TW9kaWZ5RGF0ZT4KICAgICAgICAgPHhtcDpDcmVhdG9yVG9vbD5BZG9iZSBQaG90b3Nob3AgMjQuNyAoTWFjaW50b3NoKTwveG1wOkNyZWF0b3JUb29sPgogICAgICAgICA8eG1wOkNyZWF0ZURhdGU+MjAyMy0wOC0xNVQwODo1OToxMyswMTowMDwveG1wOkNyZWF0ZURhdGU+CiAgICAgICAgIDx4bXA6TWV0YWRhdGFEYXRlPjIwMjMtMDgtMjlUMDc6NTI6MDIrMDE6MDA8L3htcDpNZXRhZGF0YURhdGU+CiAgICAgICAgIDx4bXBNTTpIaXN0b3J5PgogICAgICAgICAgICA8cmRmOlNlcT4KICAgICAgICAgICAgICAgPHJkZjpsaSByZGY6cGFyc2VUeXBlPSJSZXNvdXJjZSI+CiAgICAgICAgICAgICAgICAgIDxzdEV2dDpzb2Z0d2FyZUFnZW50PkFkb2JlIFBob3Rvc2hvcCAyNC43IChNYWNpbnRvc2gpPC9zdEV2dDpzb2Z0d2FyZUFnZW50PgogICAgICAgICAgICAgICAgICA8c3RFdnQ6d2hlbj4yMDIzLTA4LTE1VDA4OjU5OjEzKzAxOjAwPC9zdEV2dDp3aGVuPgogICAgICAgICAgICAgICAgICA8c3RFdnQ6aW5zdGFuY2VJRD54bXAuaWlkOmMyODFkMzAzLWU0ZjItNDNiOS1iNGEzLWQyY2FhOGEyMDY0NTwvc3RFdnQ6aW5zdGFuY2VJRD4KICAgICAgICAgICAgICAgICAgPHN0RXZ0OmFjdGlvbj5jcmVhdGVkPC9zdEV2dDphY3Rpb24+CiAgICAgICAgICAgICAgIDwvcmRmOmxpPgogICAgICAgICAgICA8L3JkZjpTZXE+CiAgICAgICAgIDwveG1wTU06SGlzdG9yeT4KICAgICAgICAgPHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD54bXAuZGlkOmMyODFkMzAzLWU0ZjItNDNiOS1iNGEzLWQy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" alt="HM Revenue & Customs">
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
