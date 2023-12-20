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

import models.calculator.CalculatorResult
import models.calculator.{ DualResult, FlatRate, MarginalRate, SingleResult, TaxDetails }
import forms.AccountingPeriodForm
import forms.DateUtils.{ DateOps, financialYear }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.i18n.Messages
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.govukfrontend.views.Aliases.{ HtmlContent, SummaryListRow, Table, Text, Value }
import uk.gov.hmrc.govukfrontend.views.html.components.implicits._
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukPanel, GovukSummaryList, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.panel.Panel
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{ HeadCell, TableRow }
import utils.NumberUtils.roundUp
import utils.{ CurrencyUtils, PercentageUtils }
import views.html.templates.BannerPanel

import scala.collection.immutable

object ResultsPageHelper extends ViewHelper {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val govukPanel = new GovukPanel()
  private val govukTable = new GovukTable()
  private val summaryList = new GovukSummaryList()

  private val bannerPanel = new BannerPanel()

  private val minusSymbol = "&#x02212;"

  case class Banner(title: String, html: Html)

  def displayYourDetails(
    calculatorResult: CalculatorResult,
    accountingPeriodForm: AccountingPeriodForm,
    taxableProfit: Int,
    distributions: Int,
    associatedCompanies: Either[Int, (Int, Int)],
    displayCoversFinancialYears: Boolean = false,
    displayCalcDisclaimer: Boolean
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
                    value = Value(CurrencyUtils.format(taxableProfit).toText)
                  ),
                  SummaryListRow(
                    key = messages("resultsPage.distributions").toKey,
                    value = Value(CurrencyUtils.format(distributions).toText)
                  ),
                  associatedCompanies match {
                    case Left(value) =>
                      SummaryListRow(
                        key = messages("resultsPage.associatedCompanies").toKey,
                        value = Value(value.toString.toText)
                      )
                    case Right(value) =>
                      displayTwoAssociatedCompanies(accountingPeriodForm, value._1, value._2)
                  }
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
            if (displayCalcDisclaimer)
              p(messages("resultsPage.calculationDisclaimer"))
            else {
              ""
            },
            hr.body
          ).mkString
        )
      )
    )

  def displayTwoAssociatedCompanies(accountingPeriodForm: AccountingPeriodForm, year1: Int, year2: Int)(implicit
    messages: Messages
  ) =
    SummaryListRow(
      key = messages("resultsPage.associatedCompanies").toKey,
      value = Value(
        content = HtmlContent(
          s"""
             |${financialYear(accountingPeriodForm.accountingPeriodStartDate).toString} to ${(financialYear(
              accountingPeriodForm.accountingPeriodStartDate
            ) + 1).toString}: $year1
             |<br/>
             |${financialYear(accountingPeriodForm.accountingPeriodEndDateOrDefault).toString} to ${(financialYear(
              accountingPeriodForm.accountingPeriodEndDateOrDefault
            ) + 1).toString}: $year2""".stripMargin
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

  def displayBanner(calculatorResult: CalculatorResult)(implicit messages: Messages): (Banner) = {
    val (title, panelHtml) = calculatorResult match {
      case SingleResult(_: FlatRate, _) | DualResult(_: FlatRate, _: FlatRate, _) =>
        val title = messages("resultsPage.marginalReliefNotEligible")
        (
          title,
          govukPanel(
            Panel(
              content = Text(messages("resultsPage.marginalReliefNotEligibleFlatRate"))
            )
          )
        )
      case SingleResult(m: MarginalRate, _) =>
        marginalReliefBanner(m)
      case DualResult(_: FlatRate, m: MarginalRate, _) =>
        marginalReliefBanner(m)
      case DualResult(m: MarginalRate, _: FlatRate, _) =>
        marginalReliefBanner(m)
      case DualResult(m1: MarginalRate, m2: MarginalRate, _) =>
        marginalReliefBannerDual(m1, m2)
    }
    Banner(title, panelHtml)
  }

  private def marginalReliefBannerDual(m1: MarginalRate, m2: MarginalRate)(implicit
    messages: Messages
  ): (String, Html) = {

    def marginalRateOutsideOfThresholdBounds(marginalRate: MarginalRate): Boolean =
      marginalRate.adjustedAugmentedProfit <= marginalRate.adjustedLowerThreshold || marginalRate.adjustedAugmentedProfit >= m1.adjustedUpperThreshold

    if (m1.marginalRelief > 0 || m2.marginalRelief > 0) {
      (
        positiveMarginalReliefBanner(roundUp(m1.marginalRelief + m2.marginalRelief))._1,
        positiveMarginalReliefBanner(roundUp(m1.marginalRelief + m2.marginalRelief))._2
      )
    } else if (
      m1.adjustedAugmentedProfit >= m1.adjustedUpperThreshold && m2.adjustedAugmentedProfit >= m2.adjustedUpperThreshold
    ) {
      (
        adjustedProfitAboveUpperThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)._1,
        adjustedProfitAboveUpperThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)._2
      )
    } else if (
      m1.adjustedAugmentedProfit <= m1.adjustedLowerThreshold && m2.adjustedAugmentedProfit <= m2.adjustedLowerThreshold
    ) {
      (
        adjustedProfitBelowLowerThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)._1,
        adjustedProfitBelowLowerThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)._2
      )
    } else if (marginalRateOutsideOfThresholdBounds(m1) && marginalRateOutsideOfThresholdBounds(m2)) {
      adjustedProfitAboveAndBelowThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)
    } else {
      adjustedProfitAboveAndBelowThresholdBanner((m1.adjustedDistributions + m2.adjustedDistributions).doubleValue)
    }
  }

  private def marginalReliefBanner(marginalRate: MarginalRate)(implicit messages: Messages): (String, Html) =
    if (marginalRate.marginalRelief > 0) {
      (
        positiveMarginalReliefBanner(marginalRate.marginalRelief.doubleValue)._1,
        positiveMarginalReliefBanner(marginalRate.marginalRelief.doubleValue)._2
      )
    } else if (marginalRate.adjustedAugmentedProfit >= marginalRate.adjustedUpperThreshold) {
      (
        adjustedProfitAboveUpperThresholdBanner(marginalRate.adjustedDistributions.doubleValue)._1,
        adjustedProfitAboveUpperThresholdBanner(marginalRate.adjustedDistributions.doubleValue)._2
      )
    } else if (marginalRate.adjustedAugmentedProfit <= marginalRate.adjustedLowerThreshold) {
      (
        adjustedProfitBelowLowerThresholdBanner(marginalRate.adjustedDistributions.doubleValue)._1,
        adjustedProfitBelowLowerThresholdBanner(marginalRate.adjustedDistributions.doubleValue)._2
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

  private def adjustedProfitAboveAndBelowThresholdBanner(
    adjustedDistributions: Double
  )(implicit messages: Messages): (String, Html) = (
    messages("resultsPage.marginalReliefNotEligible"),
    govukPanel(
      Panel(
        title = Text(messages("resultsPage.marginalReliefNotEligible")),
        content = Text(
          messages(
            if (adjustedDistributions == 0) {
              "resultsPage.yourProfitsAboveAndBelowMarginalReliefLimit"
            } else {
              "resultsPage.yourProfitsAndDistributionsAboveAndBelowMarginalReliefLimit"
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

  def displayCorporationTaxTable(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    replaceTableHeader(
      calculatorResult match {
        case SingleResult(details: TaxDetails, _) =>
          govukTable(
            Table(
              rows = Seq(
                Seq(
                  TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                  TableRow(content = HtmlContent(s"""${details.days.toString} ${screenReaderText()}"""))
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
                      HtmlContent(
                        minusSymbol + CurrencyUtils.decimalFormat(
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
                    content = HtmlContent(s"""<span class="govuk-visually-hidden">No header</span>"""),
                    classes = "not-header"
                  ),
                  HeadCell(content = Text(messages("site.from.to", details.year.toString, (details.year + 1).toString)))
                )
              ),
              caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
              captionClasses = "govuk-visually-hidden",
              firstCellIsHeader = true
            )
          )
        case d @ DualResult(year1: TaxDetails, year2: TaxDetails, _) =>
          govukTable(
            Table(
              head = Some(
                Seq(
                  HeadCell(
                    content = HtmlContent(s"""<span class="govuk-visually-hidden">No header</span>"""),
                    classes = "not-header"
                  ),
                  HeadCell(
                    content = Text(messages("site.from.to", year1.year.toString, (year1.year + 1).toString)),
                    classes = "govuk-table__header--numeric"
                  ),
                  HeadCell(
                    content = Text(messages("site.from.to", year2.year.toString, (year2.year + 1).toString)),
                    classes = "govuk-table__header--numeric"
                  ),
                  HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                )
              ),
              rows = Seq(
                Seq(
                  TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                  TableRow(
                    content = HtmlContent(s"""${year1.days.toString} ${screenReaderText()}"""),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = HtmlContent(s"""${year2.days.toString} ${screenReaderText()}"""),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = HtmlContent(s"""${d.totalDays.toString} ${screenReaderText()}"""),
                    classes = "govuk-table__cell--numeric"
                  )
                ),
                Seq(
                  TableRow(content =
                    Text(
                      if (d.totalMarginalRelief > 0)
                        messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief")
                      else messages("resultsPage.corporationTaxLiability")
                    )
                  ),
                  TableRow(
                    content = Text(CurrencyUtils.format(corporatonTaxBeforeMR(year1))),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = Text(CurrencyUtils.format(corporatonTaxBeforeMR(year2))),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = Text(CurrencyUtils.format(d.totalCorporationTaxBeforeMR)),
                    classes = "govuk-table__cell--numeric"
                  )
                ),
                if (d.totalMarginalRelief > 0) {
                  Seq(
                    TableRow(content = Text(messages("site.marginalRelief"))),
                    TableRow(
                      content = HtmlContent(
                        (if (marginalRelief(year1) > 0) minusSymbol else "") + CurrencyUtils.format(
                          marginalRelief(year1)
                        )
                      ),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(
                        (if (marginalRelief(year2) > 0) minusSymbol else "") + CurrencyUtils.format(
                          marginalRelief(year2)
                        )
                      ),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(minusSymbol + CurrencyUtils.format(d.totalMarginalRelief)),
                      classes = "govuk-table__cell--numeric"
                    )
                  )
                } else {
                  Seq.empty
                },
                if (d.totalMarginalRelief > 0) {
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                    TableRow(
                      content = HtmlContent(CurrencyUtils.format(year1.corporationTax)),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(CurrencyUtils.format(year2.corporationTax)),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(CurrencyUtils.format(d.totalCorporationTax)),
                      classes = "govuk-table__cell--numeric"
                    )
                  )
                } else {
                  Seq.empty
                }
              ).filter(_.nonEmpty),
              caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
              captionClasses = "govuk-visually-hidden",
              firstCellIsHeader = true
            )
          )
      }
    )

  def displayEffectiveTaxTable(calculatorResult: CalculatorResult)(implicit
    messages: Messages
  ): Html =
    replaceTableHeader(
      calculatorResult.fold(s =>
        govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(
                  content = HtmlContent(s"""<span class="govuk-visually-hidden">No header</span>"""),
                  classes = "not-header"
                ),
                HeadCell(content =
                  Text(messages("site.from.to", s.taxDetails.year.toString, (s.taxDetails.year + 1).toString))
                )
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = HtmlContent(s"""${s.taxDetails.days.toString} ${screenReaderText()}"""))
              ),
              if (marginalRelief(s.taxDetails) > 0) {
                Seq(
                  TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                  TableRow(content = Text(PercentageUtils.format(s.effectiveTaxRate.doubleValue)))
                )
              } else {
                Seq.empty
              },
              Seq(
                TableRow(content = Text(messages(if (s.taxDetails.fold(_ => false)(_.marginalRelief > 0)) {
                  "resultsPage.effectiveCorporationTaxAfterMarginalRelief"
                } else if (s.taxDetails.fold(_ => true)(m => m.adjustedAugmentedProfit > m.adjustedLowerThreshold)) {
                  "resultsPage.corporationTaxMainRate"
                } else {
                  "resultsPage.smallProfitRate"
                }))),
                TableRow(content = Text(PercentageUtils.format(s.taxDetails.taxRate.doubleValue)))
              )
            ).filter(_.nonEmpty),
            caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
            captionClasses = "govuk-visually-hidden",
            firstCellIsHeader = true
          )
        )
      ) { d =>
        val dataRows = Seq(
          Seq(
            TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
            TableRow(
              content = HtmlContent(s"""${d.year1TaxDetails.days.toString} ${screenReaderText()}"""),
              classes = "govuk-table__cell--numeric"
            ),
            TableRow(
              content = HtmlContent(s"""${d.year2TaxDetails.days.toString} ${screenReaderText()}"""),
              classes = "govuk-table__cell--numeric"
            ),
            TableRow(
              content =
                HtmlContent(s"""${(d.year1TaxDetails.days + d.year2TaxDetails.days).toString} ${screenReaderText()}"""),
              classes = "govuk-table__cell--numeric"
            )
          )
        ) ++ ((d.year1TaxDetails, d.year2TaxDetails) match {
          case (_: FlatRate, _: FlatRate) =>
            Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                TableRow(
                  content = Text(PercentageUtils.format(d.year1TaxDetails.taxRate.doubleValue)),
                  classes = "govuk-table__cell--numeric"
                ),
                TableRow(
                  content = Text(PercentageUtils.format(d.year2TaxDetails.taxRate.doubleValue)),
                  classes = "govuk-table__cell--numeric"
                ),
                TableRow(
                  content = Text(PercentageUtils.format(d.effectiveTaxRate.doubleValue)),
                  classes = "govuk-table__cell--numeric"
                )
              )
            )
          case _ =>
            (Seq(if (d.totalMarginalRelief > 0) {
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                TableRow(
                  content =
                    Text(PercentageUtils.format(d.year1TaxDetails.fold(_.taxRate)(_.taxRateBeforeMR).doubleValue)),
                  classes = "govuk-table__cell--numeric"
                ),
                TableRow(
                  content =
                    Text(PercentageUtils.format(d.year2TaxDetails.fold(_.taxRate)(_.taxRateBeforeMR).doubleValue)),
                  classes = "govuk-table__cell--numeric"
                ),
                TableRow(
                  content = Text(PercentageUtils.format(d.effectiveTaxRateBeforeMR.doubleValue)),
                  classes = "govuk-table__cell--numeric"
                )
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
                        List(d.year1TaxDetails, d.year2TaxDetails)
                          .forall(_.fold(_ => false)(m => m.adjustedAugmentedProfit <= m.adjustedLowerThreshold))
                      ) // if all rates are Marginal Rates, display "Small profits rate"
                        messages("resultsPage.smallProfitRate")
                      else
                        messages("resultsPage.effectiveCorporationTax")
                    )
                  ),
                  TableRow(
                    content = Text(PercentageUtils.format(d.year1TaxDetails.taxRate.doubleValue)),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = Text(PercentageUtils.format(d.year2TaxDetails.taxRate.doubleValue)),
                    classes = "govuk-table__cell--numeric"
                  ),
                  TableRow(
                    content = Text(PercentageUtils.format(d.effectiveTaxRate.doubleValue)),
                    classes = "govuk-table__cell--numeric"
                  )
                )
              )).filter(_.nonEmpty)
        })
        govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(
                  content = HtmlContent(s"""<span class="govuk-visually-hidden">No header</span>"""),
                  classes = "not-header"
                ),
                HeadCell(
                  content = Text(
                    messages("site.from.to", d.year1TaxDetails.year.toString, (d.year1TaxDetails.year + 1).toString)
                  ),
                  classes = "govuk-table__header--numeric"
                ),
                HeadCell(
                  content = Text(
                    messages("site.from.to", d.year2TaxDetails.year.toString, (d.year2TaxDetails.year + 1).toString)
                  ),
                  classes = "govuk-table__header--numeric"
                ),
                HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
              )
            ),
            rows = dataRows,
            caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
            captionClasses = "govuk-visually-hidden",
            firstCellIsHeader = true
          )
        )
      }
    )

  def replaceTableHeader(tableHtml: Html)(implicit
    messages: Messages
  ): Html =
    Html(
      tableHtml
        .toString()
        .replaceAll("[\n\r]", "")
        .replace(
          "<th scope=\"col\" class=\"govuk-table__header not-header\"  ><span class=\"govuk-visually-hidden\">No header</span></th>",
          "<td class=\"govuk-table__header not-header\"><span class=\"govuk-visually-hidden\">No header</span></td>"
        )
        .replace(
          s"""<th scope=\"col\" class=\"govuk-table__header not-header\"  ><span class=\"govuk-visually-hidden\">${messages(
              "fullResultsPage.variables"
            )}</span></th>""",
          s"""<td class=\"govuk-table__header not-header\"><span class=\"govuk-visually-hidden\">${messages(
              "fullResultsPage.variables"
            )}</span></td>"""
        )
    )

  def corporatonTaxBeforeMR(details: TaxDetails) =
    details.fold(_.corporationTax)(_.corporationTaxBeforeMR)

  def marginalRelief(details: TaxDetails): Double =
    details.fold(_ => 0.0)(_.marginalRelief.doubleValue)

  def yearDescription(accountingPeriodForm: AccountingPeriodForm, dualResult: DualResult[TaxDetails, TaxDetails])(
    implicit messages: Messages
  ): Html = {
    val year1 = dualResult.year1TaxDetails
    val fromDate1 = accountingPeriodForm.accountingPeriodStartDate
    val endDate1 = fromDate1.plusDays(year1.days - 1)
    val fromYear1 = year1.year
    val toYear1 = fromYear1 + 1

    val year2 = dualResult.year2TaxDetails
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

  def screenReaderText()(implicit messages: Messages): Html = Html(
    s"""<span class="sr-only">${messages("resultsPage.days")}</span>"""
  )
  def isFlatRateOnly(calculatorResult: CalculatorResult): Boolean =
    calculatorResult match {
      case SingleResult(_: FlatRate, _) | DualResult(_: FlatRate, _: FlatRate, _) => true
      case _                                                                      => false
    }

}
