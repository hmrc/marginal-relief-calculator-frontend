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
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukPanel, GovukSummaryList, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.{ CurrencyUtils, DecimalToFractionUtils, PercentageUtils }
import uk.gov.hmrc.govukfrontend.views.html.components.implicits._

import java.time.Year
import scala.collection.immutable

object ResultsPageHelper extends ViewHelper {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val govukPanel = new GovukPanel()
  private val govukTable = new GovukTable()
  private val summaryList = new GovukSummaryList()

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
        h1(messages(messages("resultsPage.yourDetails"))),
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
                  SummaryListRow(
                    key = messages("resultsPage.associatedCompanies").toKey,
                    value = Value(associatedCompanies.toString.toText)
                  )
                ),
                classes = "govuk-summary-list--no-border"
              )
            ).body,
            calculatorResult
              .fold(single => Html("")) { dual =>
                Html(
                  Seq(
                    headingS(messages("resultsPage.2years.period.heading")).body,
                    yearDescription(accountingPeriodForm, dual).body
                  ).mkString
                )
              }
              .body,
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
                accountingPeriodForm.accountingPeriodStartDate.formatDate,
                accountingPeriodForm.accountingPeriodEndDate.get.formatDate
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
          accountingPeriodForm.accountingPeriodStartDate.formatDate,
          accountingPeriodForm.accountingPeriodEndDate.get.formatDate
        )
      )
    }

  def displayBanner(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    calculatorResult match {
      case SingleResult(_: FlatRate) | DualResult(_: FlatRate, _: FlatRate) =>
        govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.marginalReliefNotApplicable"))
          )
        )
      case SingleResult(m: MarginalRate) =>
        marginalReliefBanner(m)
      case DualResult(_: FlatRate, m: MarginalRate) =>
        marginalReliefBanner(m)
      case DualResult(m: MarginalRate, _: FlatRate) =>
        marginalReliefBanner(m)
      case DualResult(_: MarginalRate, _: MarginalRate) =>
        HtmlFormat.empty
    }

  private def marginalReliefBanner(marginalRate: MarginalRate)(implicit messages: Messages): Html =
    if (marginalRate.marginalRelief > 0) {
      govukPanel(
        Panel(
          title = HtmlContent(s"""<span class="govuk-!-font-weight-regular">${messages(
              "resultsPage.marginalReliefForAccPeriodIs"
            )}</span>"""),
          content = HtmlContent(
            s"""<span class="govuk-!-font-weight-bold">${CurrencyUtils.format(marginalRate.marginalRelief)}</span>"""
          )
        )
      )
    } else if (marginalRate.adjustedAugmentedProfit >= marginalRate.adjustedUpperThreshold) {
      govukPanel(
        Panel(
          title = Text(messages("resultsPage.marginalReliefNotEligible")),
          content = Text(
            messages(
              if (marginalRate.adjustedDistributions == 0) {
                "resultsPage.yourProfitsAboveMarginalReliefLimit"
              } else {
                "resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"
              }
            )
          )
        )
      )
    } else if (marginalRate.adjustedAugmentedProfit <= marginalRate.adjustedLowerThreshold) {
      govukPanel(
        Panel(
          title = Text(messages("resultsPage.marginalReliefNotEligible")),
          content = Text(
            messages(
              if (marginalRate.adjustedDistributions == 0) {
                "resultsPage.yourProfitsBelowMarginalReliefLimit"
              } else {
                "resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"
              }
            )
          )
        )
      )
    } else {
      val message =
        "Marginal relief was 0, but augmented profit was neither <= lower-threshold or >= upper-threshold. Probably a rounding issue!"
      logger.error(message)
      throw new UnsupportedOperationException(message)
    }

  def displayFullCalculationResult(
    calculatorResult: CalculatorResult,
    associatedCompanies: Int,
    taxableProfit: Long,
    distributions: Long,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    def nonTabDisplay(taxDetails: Seq[TaxDetails]) = {
      val htmlString = taxDetails.map { td =>
        val year = td.year
        val days = td.days
        td.fold { flat =>
          Seq(
            s"""<h3 class="govuk-heading-m" style="margin-bottom: 4px;">${messages(
                "fullResultsPage.forFinancialYear",
                year.toString,
                (year + 1).toString,
                days
              )}</h2>""",
            s"""<p class="govuk-body">${messages(
                "fullResultsPage.marginalReliefNotAvailable",
                year.toString,
                (year + 1).toString
              )}</p>"""
          ).mkString
        } { marginal =>
          Seq(
            s"""<h3 class="govuk-heading-m" style="margin-bottom: 4px;">${messages(
                "fullResultsPage.forFinancialYear",
                year.toString,
                (year + 1).toString,
                days
              )}</h2>""",
            displayFullFinancialYearTable(marginal, associatedCompanies, taxableProfit, distributions, config)
          ).mkString
        }
      }.mkString

      Html(htmlString)
    }

    calculatorResult.fold(single => nonTabDisplay(Seq(single.details))) { dual =>
      def tabBtn(year: Int) =
        s"""<li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
           |      <a class="govuk-tabs__tab" href="#year$year">
           |        ${messages("site.from.to", year.toString, (year + 1).toString)}
           |      </a>
           |    </li>""".stripMargin

      def tabContent(marginalRate: MarginalRate) = {
        val year = marginalRate.year
        s"""<div class="govuk-tabs__panel" id="year$year">
           |    <h2 class="govuk-heading-m">${messages(
            "fullResultsPage.forFinancialYear",
            year.toString,
            (year + 1).toString,
            marginalRate.days
          )}</h2>
           |    ${displayFullFinancialYearTable(marginalRate, associatedCompanies, taxableProfit, distributions, config)}
           |  </div>""".stripMargin
      }

      def tabDisplay(marginalRates: Seq[MarginalRate]) =
        Html(s"""
                |<div class="govuk-tabs" data-module="govuk-tabs">
                |  <h2 class="govuk-tabs__title">
                |    Financial year results
                |  </h2>
                |  <ul class="govuk-tabs__list">
                |    ${marginalRates.map(_.year).map(tabBtn).mkString}
                |  </ul>
                |  ${marginalRates.map(tabContent).mkString}
                |</div>
                |""".stripMargin)

      dual.year1 -> dual.year2 match {
        case (y1: MarginalRate, y2: MarginalRate) => tabDisplay(Seq(y1, y2))
        case (y1: MarginalRate, y2: FlatRate)     => nonTabDisplay(Seq(y1, y2))
        case (y1: FlatRate, y2: MarginalRate)     => nonTabDisplay(Seq(y1, y2))
        case _                                    => throw new RuntimeException("Both financial years are flat rate")
      }

    }
  }

  private def displayFullFinancialYearTable(
    marginalRate: MarginalRate,
    associatedCompanies: Int,
    taxableProfit: Long,
    distributions: Long,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    val yearConfig = config(marginalRate.year) match {
      case x: FlatRateConfig       => throw new RuntimeException("Configuration is flat where it should be marginal")
      case x: MarginalReliefConfig => x
    }

    def boldRow(text: String) = TableRow(content = Text(text), classes = "govuk-!-font-weight-bold")

    val days = marginalRate.days

    val upperLimit = CurrencyUtils.format(yearConfig.upperThreshold)

    val dayMsg = messages("fullResultsPage.day.singular")
    val daysMsg = messages("fullResultsPage.day.plural")

    val daysString = days match {
      case 1 => days + s" $dayMsg"
      case _ => days + s" $daysMsg"
    }

    val associatedCompaniesText = associatedCompanies match {
      case 1 => 1 + " " + messages("fullResultsPage.associatedCompany.singular")
      case x => x + " " + messages("fullResultsPage.associatedCompany.plural")
    }

    val originalCompanyMsg = messages("fullResultsPage.oneOriginalCompany")

    val upperLimitMsg = messages("fullResultsPage.upperLimit")

    val pointOneCompaniesCalcText = s"($associatedCompaniesText + $originalCompanyMsg)"

    def cur = CurrencyUtils.format _

    val fraction = {
      val f = DecimalToFractionUtils.toFraction(yearConfig.marginalReliefFraction)
      f.numerator + " ÷ " + f.denominator
    }
    val adjustedUpperLimit = marginalRate.adjustedUpperThreshold

    val taxableProfitIncludingDistributions = marginalRate.adjustedAugmentedProfit

    val daysInYear = Year.of(marginalRate.year).length()

    govukTable(
      Table(
        rows = Seq(
          Seq(
            boldRow("1"),
            TableRow(content = Text(messages("fullResultsPage.financialYear.adjustUpperLimit"))),
            TableRow(content =
              Text(s"$upperLimit $upperLimitMsg × ($daysString ÷ $daysInYear $daysMsg) ÷ $pointOneCompaniesCalcText")
            ),
            TableRow(content = Text(cur(adjustedUpperLimit)))
          ),
          Seq(
            boldRow("2"),
            TableRow(content = Text(messages("fullResultsPage.financialYear.taxableProfit"))),
            TableRow(content = Text(s"${cur(taxableProfit)} × ($daysString ÷ $daysInYear $daysMsg)")),
            TableRow(content = Text(cur(marginalRate.adjustedProfit)))
          ),
          Seq(
            boldRow("3"),
            TableRow(content = Text(messages("fullResultsPage.financialYear.taxableProfitDistributions"))),
            TableRow(content =
              Text(
                s"${cur(marginalRate.adjustedProfit)} + ${cur(distributions)} × ($daysString ÷ $daysInYear $daysMsg)"
              )
            ),
            TableRow(content = Text(cur(taxableProfitIncludingDistributions)))
          ),
          Seq(
            boldRow("4"),
            TableRow(content = Text(messages("fullResultsPage.financialYear.marginalReliefFraction"))),
            TableRow(content = Text(messages("fullResultsPage.financialYear.marginalReliefFraction.description"))),
            TableRow(content = Text(fraction))
          ),
          Seq(
            boldRow("5"),
            TableRow(content = Text(messages("fullResultsPage.financialYear.fullCalculation"))),
            TableRow(content =
              Text(s"""(${cur(adjustedUpperLimit)} - ${cur(taxableProfitIncludingDistributions)}) × (${cur(
                  marginalRate.adjustedProfit
                )} ÷ ${cur(taxableProfitIncludingDistributions)}) × ($fraction)""")
            ),
            TableRow(content = Text(CurrencyUtils.format(marginalRelief(marginalRate))))
          )
        ),
        head = Some(
          Seq(
            HeadCell(),
            HeadCell(),
            HeadCell(content = Text("Calculation")),
            HeadCell(content = Text("Result"))
          )
        )
      )
    )
  }

  def displayCorporationTaxTable(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    calculatorResult match {
      case SingleResult(details: TaxDetails) =>
        govukTable(
          Table(
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text(details.days.toString))
              ),
              Seq(
                TableRow(content =
                  Text(
                    if (marginalRelief(details) > 0)
                      messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief")
                    else messages("resultsPage.corporationTaxLiability")
                  )
                ),
                TableRow(content = Text(CurrencyUtils.format(corporatonTaxBeforeMR(details))))
              ),
              if (marginalRelief(details) > 0) {
                Seq(
                  TableRow(content = Text(messages("site.marginalRelief"))),
                  TableRow(content =
                    Text(
                      "-" + CurrencyUtils.format(
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
                  TableRow(content = Text(CurrencyUtils.format(details.corporationTax)))
                )
              } else {
                Seq.empty
              }
            ).filter(_.nonEmpty),
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", details.year.toString, (details.year + 1).toString)))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      case d @ DualResult(year1: TaxDetails, year2: TaxDetails) =>
        govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", year1.year.toString, (year1.year + 1).toString))),
                HeadCell(content = Text(messages("site.from.to", year2.year.toString, (year2.year + 1).toString))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                TableRow(content = Text(year1.days.toString)),
                TableRow(content = Text(year2.days.toString)),
                TableRow(content = Text(d.totalDays.toString))
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
            caption = None,
            firstCellIsHeader = true
          )
        )
    }

  def displayEffectiveTaxTable(calculatorResult: CalculatorResult)(implicit
    messages: Messages
  ): Html =
    calculatorResult.fold(s =>
      govukTable(
        Table(
          head = Some(
            Seq(
              HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
              HeadCell(content = Text(messages("site.from.to", s.details.year.toString, (s.details.year + 1).toString)))
            )
          ),
          rows = Seq(
            Seq(
              TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
              TableRow(content = Text(s.details.days.toString))
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
          caption = None,
          firstCellIsHeader = true
        )
      )
    ) { d =>
      val dataRows = Seq(
        Seq(
          TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
          TableRow(content = Text(d.year1.days.toString)),
          TableRow(content = Text(d.year2.days.toString)),
          TableRow(content = Text((d.year1.days + d.year2.days).toString))
        )
      ) ++ ((d.year1, d.year2) match {
        case (_: FlatRate, _: FlatRate) =>
          Seq(
            Seq(
              TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
              TableRow(content = Text(PercentageUtils.format(d.year1.taxRate))),
              TableRow(content = Text(PercentageUtils.format(d.year2.taxRate))),
              TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRate)))
            )
          )
        case _ =>
          (Seq(if (d.totalMarginalRelief > 0) {
            Seq(
              TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
              TableRow(content = Text(PercentageUtils.format(d.year1.fold(_.taxRate)(_.taxRateBeforeMR)))),
              TableRow(content = Text(PercentageUtils.format(d.year2.fold(_.taxRate)(_.taxRateBeforeMR)))),
              TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRateBeforeMR)))
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
                TableRow(content = Text(PercentageUtils.format(d.year1.taxRate))),
                TableRow(content = Text(PercentageUtils.format(d.year2.taxRate))),
                TableRow(content = Text(PercentageUtils.format(d.effectiveTaxRate)))
              )
            )).filter(_.nonEmpty)
      })
      govukTable(
        Table(
          head = Some(
            Seq(
              HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
              HeadCell(content = Text(messages("site.from.to", d.year1.year.toString, (d.year1.year + 1).toString))),
              HeadCell(content = Text(messages("site.from.to", d.year2.year.toString, (d.year2.year + 1).toString))),
              HeadCell(content = Text(messages("site.overall")))
            )
          ),
          rows = dataRows,
          caption = None,
          firstCellIsHeader = true
        )
      )
    }

  private def corporatonTaxBeforeMR(details: TaxDetails) =
    details.fold(_.corporationTax)(_.corporationTaxBeforeMR)

  private def marginalRelief(details: TaxDetails): Double =
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
    val endDate2 = accountingPeriodForm.accountingPeriodEndDate.getOrElse(
      accountingPeriodForm.accountingPeriodStartDate.plusMonths(12)
    )
    val fromYear2 = year2.year
    val toYear2 = fromYear2 + 1

    p(
      messages("site.from.to", fromYear1.toString, toYear1.toString) + ": " +
        messages("site.from.to", fromDate1.formatDateFull, endDate1.formatDateFull) + "<br/>" +
        messages("site.from.to", fromYear2.toString, toYear2.toString) + ": " +
        messages("site.from.to", fromDate2.formatDateFull, endDate2.formatDateFull)
    )
  }
}
