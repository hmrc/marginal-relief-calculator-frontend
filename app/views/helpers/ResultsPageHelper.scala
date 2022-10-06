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
import forms.DateUtils.DateOps
import org.slf4j.{ Logger, LoggerFactory }
import play.api.i18n.Messages
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.implicits._
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukPanel, GovukSummaryList, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.NumberUtils.roundUp
import utils.{ CurrencyUtils, PercentageUtils }
import views.html.templates.BannerPanel

import scala.collection.{ immutable, mutable }

object ResultsPageHelper extends ViewHelper {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val govukPanel = new GovukPanel()
  private val govukTable = new GovukTable()
  private val summaryList = new GovukSummaryList()

  private val bannerPanel = new BannerPanel()

  def displayYourDetails(
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    taxableProfit: Int,
    distributions: Int,
    associatedCompanies: Int,
    displayCoversFinancialYears: Boolean = false
  )(implicit messages: Messages): Html =
    HtmlFormat.fill(
      immutable.Seq(
        h2(text = messages("resultsPage.yourDetails"), styles = "margin-bottom: 4px"),
        Html(
          Seq(
            summaryList(
              SummaryList(
                rows = Seq(
                  SummaryListRow(
                    key = messages("resultsPage.accountPeriod").toKey,
                    value = Value(
                      displayAccountingPeriodText(
                        calculatorResult,
                        accountingPeriodForm,
                        displayCoversFinancialYears,
                        messages
                      )
                    )
                  ),
                  SummaryListRow(
                    key = messages("resultsPage.companysProfit").toKey,
                    value = Value(CurrencyUtils.decimalFormat(taxableProfit).toText)
                  ),
                  SummaryListRow(
                    key = messages("resultsPage.distributions").toKey,
                    value = Value(CurrencyUtils.decimalFormat(distributions).toText)
                  ),
                  SummaryListRow(
                    key = messages("resultsPage.associatedCompanies").toKey,
                    value = Value(associatedCompanies.toString.toText)
                  )
                ),
                classes = "govuk-summary-list--no-border"
              )
            ).body,
            if (!displayCoversFinancialYears)
              calculatorResult
                .fold(single => Html("")) { dual =>
                  Html(
                    Seq(
                      headingS(messages("resultsPage.2years.period.heading")).body,
                      yearDescription(accountingPeriodForm, dual).body
                    ).mkString
                  )
                }
                .body
            else "",
            hr.body
          ).mkString
        )
      )
    )

  private def displayAccountingPeriodText(
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    displayCoversFinancialYears: Boolean,
    messages: Messages
  ) =
    if (displayCoversFinancialYears && calculatorResult.fold(_ => false)(_ => true)) {
      HtmlContent(
        HtmlFormat.fill(
          immutable.Seq(
            p(
              messages(
                "site.from.to",
                accountingPeriodForm.accountingPeriodStartDate.formatDateFull,
                accountingPeriodForm.accountingPeriodEndDateOrDefault.formatDateFull
              )
            ),
            calculatorResult.fold(_ => HtmlFormat.empty)(_ => p(messages("resultsPage.covers2FinancialYears")))
          )
        )
      )
    } else {
      HtmlContent(
        messages(
          "site.from.to",
          accountingPeriodForm.accountingPeriodStartDate.formatDateFull,
          accountingPeriodForm.accountingPeriodEndDateOrDefault.formatDateFull
        )
      )
    }

  def displayBanner(calculatorResult: CalculatorResult)(implicit messages: Messages): (String, Html) = {
    val (title, panelHtml) = calculatorResult match {
      case SingleResult(_: FlatRate) | DualResult(_: FlatRate, _: FlatRate) =>
        val title = messages("resultsPage.marginalReliefNotEligible")
        (
          title,
          govukPanel(
            Panel(
              title = Text(title),
              content = Text(messages("resultsPage.marginalReliefNotApplicable"))
            )
          )
        )
      case SingleResult(m: MarginalRate) =>
        marginalReliefBanner(m)
      case DualResult(_: FlatRate, m: MarginalRate) =>
        marginalReliefBanner(m)
      case DualResult(m: MarginalRate, _: FlatRate) =>
        marginalReliefBanner(m)
      case DualResult(m1: MarginalRate, m2: MarginalRate) =>
        marginalReliefBannerDual(m1, m2)
    }
    (title, addBannerScreenReader(calculatorResult, panelHtml))
  }

  private def marginalReliefBannerDual(m1: MarginalRate, m2: MarginalRate)(implicit
    messages: Messages
  ): (String, Html) =
    if (m1.marginalRelief > 0 || m2.marginalRelief > 0) {
      (
        positiveMarginalReliefBanner(roundUp(BigDecimal(m1.marginalRelief + m2.marginalRelief)))._1,
        positiveMarginalReliefBanner(roundUp(BigDecimal(m1.marginalRelief + m2.marginalRelief)))._2
      )
    } else if (
      m1.adjustedAugmentedProfit >= m1.adjustedUpperThreshold && m2.adjustedAugmentedProfit >= m2.adjustedUpperThreshold
    ) {
      (
        adjustedProfitAboveUpperThresholdBanner(m1.adjustedDistributions + m2.adjustedDistributions)._1,
        adjustedProfitAboveUpperThresholdBanner(m1.adjustedDistributions + m2.adjustedDistributions)._2
      )
    } else if (
      m1.adjustedAugmentedProfit <= m1.adjustedLowerThreshold && m2.adjustedAugmentedProfit <= m2.adjustedLowerThreshold
    ) {
      (
        adjustedProfitBelowLowerThresholdBanner(m1.adjustedDistributions + m2.adjustedDistributions)._1,
        adjustedProfitBelowLowerThresholdBanner(m1.adjustedDistributions + m2.adjustedDistributions)._2
      )
    } else {
      val message =
        "Marginal relief was 0, however adjusted profits for one year was below lower threshold and the other year was above upper threshold"
      logger.error(message)
      throw new UnsupportedOperationException(message)
    }

  private def marginalReliefBanner(marginalRate: MarginalRate)(implicit messages: Messages): (String, Html) =
    if (marginalRate.marginalRelief > 0) {
      (
        positiveMarginalReliefBanner(marginalRate.marginalRelief)._1,
        positiveMarginalReliefBanner(marginalRate.marginalRelief)._2
      )
    } else if (marginalRate.adjustedAugmentedProfit >= marginalRate.adjustedUpperThreshold) {
      (
        adjustedProfitAboveUpperThresholdBanner(marginalRate.adjustedDistributions)._1,
        adjustedProfitAboveUpperThresholdBanner(marginalRate.adjustedDistributions)._2
      )
    } else if (marginalRate.adjustedAugmentedProfit <= marginalRate.adjustedLowerThreshold) {
      (
        adjustedProfitBelowLowerThresholdBanner(marginalRate.adjustedDistributions)._1,
        adjustedProfitBelowLowerThresholdBanner(marginalRate.adjustedDistributions)._2
      )
    } else {
      val message =
        "Marginal relief was 0, but augmented profit was neither <= lower-threshold or >= upper-threshold. Probably a rounding issue!"
      logger.error(message)
      throw new UnsupportedOperationException(message)
    }

  private def adjustedProfitBelowLowerThresholdBanner(
    adjustedDistributions: Double
  )(implicit messages: Messages): (String, Html) = (
    messages("resultsPage.marginalReliefNotEligible"),
    govukPanel(
      Panel(
        title = Text(messages("resultsPage.marginalReliefNotEligible")),
        content = Text(
          messages(
            if (adjustedDistributions == 0) {
              "resultsPage.yourProfitsBelowMarginalReliefLimit"
            } else {
              "resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"
            }
          )
        )
      )
    )
  )

  def adjustedProfitAboveUpperThresholdBanner(
    adjustedDistributions: Double
  )(implicit messages: Messages): (String, Html) = (
    messages("resultsPage.marginalReliefNotEligible"),
    govukPanel(
      Panel(
        title = Text(messages("resultsPage.marginalReliefNotEligible")),
        content = Text(
          messages(
            if (adjustedDistributions == 0) {
              "resultsPage.yourProfitsAboveMarginalReliefLimit"
            } else {
              "resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"
            }
          )
        )
      )
    )
  )

  private def positiveMarginalReliefBanner(marginalRelief: Double)(implicit messages: Messages): (String, Html) = (
    messages("resultsPage.marginalReliefForAccPeriodIs"),
    bannerPanel(
      Panel(
        title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
        content = Text(CurrencyUtils.format(marginalRelief))
      )
    )
  )

  def addBannerScreenReader(calculatorResult: CalculatorResult, bannerHtml: Html)(implicit messages: Messages): Html = {
    val master = bannerHtml.toString();
    val target = "</div>"
    val startIndex: Int = master.lastIndexOf(target)
    val stopIndex: Int = startIndex + target.length;
    val replacement = s"""<span class="sr-only">
                         |  <h2>${messages("resultsPage.corporationTaxLiability")}</h2>
                         |  <span>${CurrencyUtils.format(calculatorResult.totalCorporationTax)}</span>
                         |  ${if (calculatorResult.totalMarginalRelief > 0) {
                          s"<p>${messages(
                              "resultsPage.corporationTaxReducedFrom",
                              CurrencyUtils.format(calculatorResult.totalCorporationTaxBeforeMR),
                              CurrencyUtils.format(calculatorResult.totalMarginalRelief)
                            )}</p>"
                        }}
                         |  <h2>${messages("resultsPage.effectiveTaxRate")}</h2>
                         |  <span>${PercentageUtils.format(calculatorResult.effectiveTaxRate)}</span>
                         | ${if (calculatorResult.totalMarginalRelief > 0) {
                          s"<p>${messages("resultsPage.reducedFromAfterMR", PercentageUtils.format(calculatorResult.effectiveTaxRateBeforeMR))}</p>"
                        }}
                         |</span></div>""".stripMargin

    val builder = new mutable.StringBuilder(master)
    builder.replace(startIndex, stopIndex, replacement)
    Html(
      builder.toString()
    )
  }

  def displayCorporationTaxTable(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    replaceTableHeader(
      messages("resultsPage.corporationTaxTableScreenReaderSummary"),
      calculatorResult match {
        case SingleResult(details: TaxDetails) =>
          govukTable(
            Table(
              rows = Seq(
                Seq(
                  TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                  TableRow(content = HtmlContent(s"""${details.days.toString} $screenReaderText"""))
                ),
                Seq(
                  TableRow(content =
                    Text(
                      if (marginalRelief(details) > 0)
                        messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief")
                      else messages("resultsPage.corporationTaxLiability")
                    )
                  ),
                  TableRow(content = Text(CurrencyUtils.decimalFormat(corporatonTaxBeforeMR(details))))
                ),
                if (marginalRelief(details) > 0) {
                  Seq(
                    TableRow(content = Text(messages("site.marginalRelief"))),
                    TableRow(content =
                      Text(
                        "-" + CurrencyUtils.decimalFormat(
                          marginalRelief(details)
                        )
                      )
                    )
                  )
                } else {
                  Seq.empty
                },
                if (marginalRelief(details) > 0) {
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                    TableRow(content = Text(CurrencyUtils.decimalFormat(details.corporationTax)))
                  )
                } else {
                  Seq.empty
                }
              ).filter(_.nonEmpty),
              head = Some(
                Seq(
                  HeadCell(
                    content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>"""),
                    classes = "not-header"
                  ),
                  HeadCell(content = Text(messages("site.from.to", details.year.toString, (details.year + 1).toString)))
                )
              ),
              caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
              captionClasses = "govuk-!-display-none",
              firstCellIsHeader = true
            )
          )
        case d @ DualResult(year1: TaxDetails, year2: TaxDetails) =>
          govukTable(
            Table(
              head = Some(
                Seq(
                  HeadCell(
                    content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>"""),
                    classes = "not-header"
                  ),
                  HeadCell(content = Text(messages("site.from.to", year1.year.toString, (year1.year + 1).toString))),
                  HeadCell(content = Text(messages("site.from.to", year2.year.toString, (year2.year + 1).toString))),
                  HeadCell(content = Text(messages("site.overall")))
                )
              ),
              rows = Seq(
                Seq(
                  TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                  TableRow(content = HtmlContent(s"""${year1.days.toString} $screenReaderText""")),
                  TableRow(content = HtmlContent(s"""${year2.days.toString} $screenReaderText""")),
                  TableRow(content = HtmlContent(s"""${d.totalDays.toString} $screenReaderText"""))
                ),
                Seq(
                  TableRow(content =
                    Text(
                      if (d.totalMarginalRelief > 0) messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief")
                      else messages("resultsPage.corporationTaxLiability")
                    )
                  ),
                  TableRow(content = Text(CurrencyUtils.format(corporatonTaxBeforeMR(year1)))),
                  TableRow(content = Text(CurrencyUtils.format(corporatonTaxBeforeMR(year2)))),
                  TableRow(content = Text(CurrencyUtils.format(d.totalCorporationTaxBeforeMR)))
                ),
                if (d.totalMarginalRelief > 0) {
                  Seq(
                    TableRow(content = Text(messages("site.marginalRelief"))),
                    TableRow(content =
                      Text(
                        (if (marginalRelief(year1) > 0) "-" else "") + CurrencyUtils.format(
                          marginalRelief(year1)
                        )
                      )
                    ),
                    TableRow(content =
                      Text(
                        (if (marginalRelief(year2) > 0) "-" else "") + CurrencyUtils.format(
                          marginalRelief(year2)
                        )
                      )
                    ),
                    TableRow(content = Text("-" + CurrencyUtils.format(d.totalMarginalRelief)))
                  )
                } else {
                  Seq.empty
                },
                if (d.totalMarginalRelief > 0) {
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                    TableRow(content = Text(CurrencyUtils.format(year1.corporationTax))),
                    TableRow(content = Text(CurrencyUtils.format(year2.corporationTax))),
                    TableRow(content = Text(CurrencyUtils.format(d.totalCorporationTax)))
                  )
                } else {
                  Seq.empty
                }
              ).filter(_.nonEmpty),
              caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
              captionClasses = "govuk-!-display-none",
              firstCellIsHeader = true
            )
          )
      }
    )

  def displayEffectiveTaxTable(calculatorResult: CalculatorResult)(implicit
    messages: Messages
  ): Html =
    replaceTableHeader(
      messages("resultsPage.effectiveTaxTableScreenReaderSummary"),
      calculatorResult.fold(s =>
        govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(
                  content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>"""),
                  classes = "not-header"
                ),
                HeadCell(content =
                  Text(messages("site.from.to", s.details.year.toString, (s.details.year + 1).toString))
                )
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = HtmlContent(s"""${s.details.days.toString} $screenReaderText"""))
              ),
              if (s.details.fold(_ => false)(_.marginalRelief > 0)) {
                Seq(
                  TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                  TableRow(content = Text(PercentageUtils.format(s.effectiveTaxRateBeforeMR)))
                )
              } else {
                Seq.empty
              },
              Seq(
                TableRow(content = Text(messages(if (s.details.fold(_ => false)(_.marginalRelief > 0)) {
                  "resultsPage.effectiveCorporationTaxAfterMarginalRelief"
                } else if (s.details.fold(_ => true)(m => m.adjustedAugmentedProfit > m.adjustedLowerThreshold)) {
                  "resultsPage.corporationTaxMainRate"
                } else {
                  "resultsPage.smallProfitRate"
                }))),
                TableRow(content = Text(PercentageUtils.format(s.details.taxRate)))
              )
            ).filter(_.nonEmpty),
            caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
            captionClasses = "govuk-!-display-none",
            firstCellIsHeader = true
          )
        )
      ) { d =>
        val dataRows = Seq(
          Seq(
            TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
            TableRow(content = HtmlContent(s"""${d.year1.days.toString} $screenReaderText"""), classes = "govuk-table__cell--numeric"),
            TableRow(content = HtmlContent(s"""${d.year2.days.toString} $screenReaderText"""), classes = "govuk-table__cell--numeric"),
            TableRow(content = HtmlContent(s"""${(d.year1.days + d.year2.days).toString} $screenReaderText"""),
              classes = "govuk-table__cell--numeric")
          )
        ) ++ ((d.year1, d.year2) match {
          case (_: FlatRate, _: FlatRate) =>
            Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                TableRow(content = Text(PercentageUtils.format(d.year1.taxRate)), classes = "govuk-table__cell--numeric"),
                TableRow(content = Text(PercentageUtils.format(d.year2.taxRate)), classes = "govuk-table__cell--numeric"),
                TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRate)), classes = "govuk-table__cell--numeric")
              )
            )
          case _ =>
            (Seq(if (d.totalMarginalRelief > 0) {
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                TableRow(content = Text(PercentageUtils.format(d.year1.fold(_.taxRate)(_.taxRateBeforeMR))), classes = "govuk-table__cell--numeric"),
                TableRow(content = Text(PercentageUtils.format(d.year2.fold(_.taxRate)(_.taxRateBeforeMR))), classes = "govuk-table__cell--numeric"),
                TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRateBeforeMR)), classes = "govuk-table__cell--numeric")
              )
            } else {
              Seq.empty
            }) ++
              Seq(
                Seq(
                  TableRow(content =
                    Text(
                      if (d.totalMarginalRelief > 0)
                        messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief")
                      else if (
                        List(d.year1, d.year2)
                          .forall(_.fold(_ => false)(m => m.adjustedAugmentedProfit <= m.adjustedLowerThreshold))
                      ) // if all rates are Marginal Rates, display "Small profits rate"
                        messages("resultsPage.smallProfitRate")
                      else
                        messages("resultsPage.effectiveCorporationTax")
                    )
                  ),
                  TableRow(content = Text(PercentageUtils.format(d.year1.taxRate)), classes = "govuk-table__cell--numeric"),
                  TableRow(content = Text(PercentageUtils.format(d.year2.taxRate)), classes = "govuk-table__cell--numeric"),
                  TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRate)), classes = "govuk-table__cell--numeric")
                )
              )).filter(_.nonEmpty)
        })
        govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(
                  content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>"""),
                  classes = "not-header"
                ),
                HeadCell(content = Text(messages("site.from.to", d.year1.year.toString, (d.year1.year + 1).toString)), classes = "govuk-table__cell--numeric"),
                HeadCell(content = Text(messages("site.from.to", d.year2.year.toString, (d.year2.year + 1).toString)), classes = "govuk-table__cell--numeric"),
                HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__cell--numeric")
              )
            ),
            rows = dataRows,
            caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
            captionClasses = "govuk-!-display-none",
            firstCellIsHeader = true
          )
        )
      }
    )

  def replaceTableHeader(tableSummary: String, tableHtml: Html): Html =
    Html(
      tableHtml
        .toString()
        .replaceAll("[\n\r]", "")
        .replace(
          "<th scope=\"col\" class=\"govuk-table__header not-header\"  ><span class=\"govuk-!-display-none\">No header</span></th>",
          "<td scope=\"col\" class=\"govuk-table__header not-header\"><span class=\"govuk-!-display-none\">No header</span></td>"
        )
        .replace(
          "<table",
          s"""<table summary="$tableSummary""""
        )
    )

  private def corporatonTaxBeforeMR(details: TaxDetails) =
    details.fold(_.corporationTax)(_.corporationTaxBeforeMR)

  def marginalRelief(details: TaxDetails): Double =
    details.fold(_ => 0.0)(_.marginalRelief)

  def yearDescription(accountingPeriodForm: AccountingPeriodForm, dualResult: DualResult)(implicit
    messages: Messages
  ): Html = {
    val year1 = dualResult.year1
    val fromDate1 = accountingPeriodForm.accountingPeriodStartDate
    val endDate1 = fromDate1.plusDays(year1.days - 1)
    val fromYear1 = year1.year
    val toYear1 = fromYear1 + 1

    val year2 = dualResult.year2
    val fromDate2 = endDate1.plusDays(1)
    val endDate2 = accountingPeriodForm.accountingPeriodEndDateOrDefault
    val fromYear2 = year2.year
    val toYear2 = fromYear2 + 1

    Html(s"""${p(
        messages("site.from.to", fromYear1.toString, toYear1.toString) + ": " +
          messages("site.from.to", fromDate1.formatDateFull, endDate1.formatDateFull),
        "govuk-body govuk-!-margin-0"
      )}
    ${p(
        messages("site.from.to", fromYear2.toString, toYear2.toString) + ": " +
          messages("site.from.to", fromDate2.formatDateFull, endDate2.formatDateFull)
      )}""")
  }

  def screenReaderText()(implicit messages: Messages) = Html(
    s"""<span class="sr-only">${messages("resultsPage.days")}</span>"""
  )
}
