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
import play.api.i18n.Messages
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukDetails, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.{ CurrencyUtils, DecimalToFractionUtils }
import scala.collection.immutable.Seq

object FullResultsPageHelper extends ViewHelper {

  private val govukTable = new GovukTable()
  private val govukDetails = new GovukDetails()

  def displayFullCalculationResult(
    calculatorResult: CalculatorResult,
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    def nonTabDisplay(taxDetails: Seq[TaxDetails], daysInAccountingPeriod: Int) = {
      taxDetails match {
        case Seq(_: FlatRate) => throw new RuntimeException("Only flat rate year is available")
        case _                => ()
      }
      val html = taxDetails.flatMap { td =>
        val year = td.year
        val days = td.days
        td.fold { _ =>
          Seq(
            h3(
              messages(
                "fullResultsPage.forFinancialYear",
                year.toString,
                (year + 1).toString,
                days
              )
            ),
            p(
              messages(
                "fullResultsPage.marginalReliefNotAvailable",
                year.toString,
                (year + 1).toString
              )
            )
          )
        } { marginal =>
          Seq(
            h3(
              messages(
                "fullResultsPage.forFinancialYear",
                year.toString,
                (year + 1).toString,
                days
              )
            ),
            displayFullFinancialYearTable(
              marginal,
              associatedCompanies,
              taxableProfit,
              distributions,
              config,
              daysInAccountingPeriod
            )
          )
        }
      }

      HtmlFormat.fill(html)
    }

    def dualResultTable(dual: DualResult) = {
      def tabBtn(year: Int) =
        s"""<li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
           |      <a class="govuk-tabs__tab" href="#year$year">
           |        ${messages("site.from.to", year.toString, (year + 1).toString)}
           |      </a>
           |    </li>""".stripMargin

      def tabContent(marginalRate: MarginalRate, daysInAccountingPeriod: Int) = {
        val year = marginalRate.year
        s"""<div class="govuk-tabs__panel" id="year$year">
           |    ${h2(
            messages(
              "fullResultsPage.forFinancialYear",
              year.toString,
              (year + 1).toString,
              marginalRate.days
            )
          )}
           |    ${displayFullFinancialYearTable(
            marginalRate,
            associatedCompanies,
            taxableProfit,
            distributions,
            config,
            daysInAccountingPeriod
          )}
           |  </div>""".stripMargin
      }

      def tabDisplay(marginalRates: Seq[MarginalRate], daysInAccountingPeriod: Int) =
        Html(s"""
                |<div class="govuk-tabs" data-module="govuk-tabs">
                |  <h2 class="govuk-tabs__title">
                |    ${messages("fullResultsPage.financialYearResults")}
                |  </h2>
                |  <ul class="govuk-tabs__list">
                |    ${marginalRates.map(_.year).map(tabBtn).mkString}
                |  </ul>
                |  ${marginalRates.map(rate => tabContent(rate, daysInAccountingPeriod)).mkString}
                |</div>
                |""".stripMargin)

      val daysInAccountingPeriod = dual.year1.days + dual.year2.days

      dual.year1 -> dual.year2 match {
        case (y1: MarginalRate, y2: MarginalRate) => tabDisplay(Seq(y1, y2), daysInAccountingPeriod)
        case (y1: MarginalRate, y2: FlatRate)     => nonTabDisplay(Seq(y1, y2), daysInAccountingPeriod)
        case (y1: FlatRate, y2: MarginalRate)     => nonTabDisplay(Seq(y1, y2), daysInAccountingPeriod)
        case _                                    => throw new RuntimeException("Both financial years are flat rate")
      }
    }

    val financialYearTables =
      calculatorResult.fold(single => nonTabDisplay(Seq(single.details), single.details.days))(dualResultTable)

    HtmlFormat.fill(
      Seq(
        financialYearTables,
        whatIsMarginalRate(calculatorResult),
        taxableProfitTable(calculatorResult, taxableProfit, distributions)
      )
    )

  }

  private def marginalReliefFormula(implicit messages: Messages): Html =
    Html(s"""<h3 class="govuk-heading-s" style="margin-bottom: 4px;">${messages(
             "fullResultsPage.marginalReliefFormula"
           )}</h3>
            |<p class="govuk-body">${messages("fullResultsPage.marginalReliefFormula.description")}</p>""".stripMargin)

  private def whatIsMarginalRate(calculatorResult: CalculatorResult)(implicit messages: Messages) = {

    val show = {
      val taxDetails = calculatorResult.fold(single => Seq(single.details))(dual => Seq(dual.year1, dual.year2))
      taxDetails.exists(taxDetails => taxDetails.fold(_ => false)(isFiveStepMarginalRate))
    }

    if (show) {
      HtmlFormat.fill(
        Seq(
          marginalReliefFormula,
          govukDetails(
            Details(
              summary = Text(messages("fullResultsPage.whatIsMarginalRateFraction")),
              content = HtmlContent(
                s"""<p>${messages("fullResultsPage.details.standardFraction")}</p>
                   |    <p>${messages("fullResultsPage.details.standardFractionExample")}</p>
                   |    <p><b>${messages("fullResultsPage.details.whatIsMarginalRate")}</b></p>
                   |    <p>${messages("fullResultsPage.details.smallProfitRate")}</p>
                   |    <p>
                   |        ${messages("fullResultsPage.details.examples.1")}<br/>
                   |        ${messages("fullResultsPage.details.examples.2")}<br/>
                   |        ${messages("fullResultsPage.details.examples.3")}<br/>
                   |        ${messages("fullResultsPage.details.examples.4")}<br/>
                   |    </p>""".stripMargin
              )
            )
          ),
          hr
        )
      )
    } else { HtmlFormat.empty }
  }

  private def isFiveStepMarginalRate(marginalRate: MarginalRate) = marginalRate.marginalRelief > 0

  private def displayFullFinancialYearTable(
    marginalRate: MarginalRate,
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig],
    daysInAccountingPeriod: Int
  )(implicit messages: Messages): Html = {

    val yearConfig = config(marginalRate.year) match {
      case x: FlatRateConfig       => throw new RuntimeException("Configuration is flat where it should be marginal")
      case x: MarginalReliefConfig => x
    }

    def boldRow(text: String) = TableRow(content = Text(text), classes = "govuk-!-font-weight-bold")

    val days = marginalRate.days

    val upperThreshold = CurrencyUtils.format(yearConfig.upperThreshold)

    val upperThresholdMsg = messages("fullResultsPage.upperLimit")

    val upperThresholdText = upperThreshold + " " + upperThresholdMsg

    val lowerThreshold = CurrencyUtils.format(yearConfig.lowerThreshold)

    val lowerThresholdMsg = messages("fullResultsPage.lowerLimit")

    val lowerThresholdText = lowerThreshold + " " + lowerThresholdMsg

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

    val pointOneCompaniesCalcText = s"($associatedCompaniesText + $originalCompanyMsg)"

    val fraction = {
      val f = DecimalToFractionUtils.toFraction(yearConfig.marginalReliefFraction)
      f.numerator + " ÷ " + f.denominator
    }

    val taxableProfitIncludingDistributions = marginalRate.adjustedAugmentedProfit

    val isProfitsAboveLowerThreshold = taxableProfitIncludingDistributions > marginalRate.adjustedLowerThreshold

    val firstThreeSteps = Seq(
      Seq(
        boldRow("1"),
        TableRow(content =
          Text(
            messages(
              if (isProfitsAboveLowerThreshold) "fullResultsPage.financialYear.adjustedUpperLimit"
              else "fullResultsPage.financialYear.adjustedLowerLimit"
            )
          )
        ),
        TableRow(content =
          Text(
            s"${if (isProfitsAboveLowerThreshold) upperThresholdText
              else lowerThresholdText} × ($daysString ÷ $daysInAccountingPeriod $daysMsg) ÷ $pointOneCompaniesCalcText"
          )
        ),
        TableRow(content =
          Text(
            CurrencyUtils.format(
              if (isProfitsAboveLowerThreshold) marginalRate.adjustedUpperThreshold
              else marginalRate.adjustedLowerThreshold
            )
          )
        )
      ),
      Seq(
        boldRow("2"),
        TableRow(content = Text(messages("fullResultsPage.financialYear.taxableProfit"))),
        TableRow(content =
          Text(s"${CurrencyUtils.format(taxableProfit)} × ($daysString ÷ $daysInAccountingPeriod $daysMsg)")
        ),
        TableRow(content = Text(CurrencyUtils.format(marginalRate.adjustedProfit)))
      ),
      Seq(
        boldRow("3"),
        TableRow(content = Text(messages("fullResultsPage.financialYear.taxableProfitDistributions"))),
        TableRow(content =
          Text(
            s"(${CurrencyUtils.format(taxableProfit)} + ${CurrencyUtils
                .format(distributions)}) × ($daysString ÷ $daysInAccountingPeriod $daysMsg)"
          )
        ),
        TableRow(content = Text(CurrencyUtils.format(taxableProfitIncludingDistributions)))
      )
    )

    def template(rows: Seq[Seq[TableRow]], description: Option[String]) = {
      val table = Table(
        rows = rows,
        head = Some(
          Seq(
            HeadCell(),
            HeadCell(),
            HeadCell(content = Text(messages("fullResultsPage.calculation"))),
            HeadCell(content = Text(messages("fullResultsPage.result")))
          )
        )
      )
      description match {
        case Some(text) => HtmlFormat.fill(Seq(p(text), govukTable(table)))
        case _          => govukTable(table)
      }
    }

    if (isFiveStepMarginalRate(marginalRate)) {
      template(
        firstThreeSteps ++
          Seq(
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
                Text(s"""(${CurrencyUtils.format(marginalRate.adjustedUpperThreshold)} - ${CurrencyUtils.format(
                    taxableProfitIncludingDistributions
                  )}) × (${CurrencyUtils.format(
                    marginalRate.adjustedProfit
                  )} ÷ ${CurrencyUtils.format(taxableProfitIncludingDistributions)}) × ($fraction)""")
              ),
              TableRow(content = Text(CurrencyUtils.format(ResultsPageHelper.marginalRelief(marginalRate))))
            )
          ),
        None
      )
    } else if (taxableProfitIncludingDistributions <= marginalRate.adjustedLowerThreshold) {
      template(
        firstThreeSteps,
        Some(s"""
            ${messages("fullResultsPage.notEligibleBelowLowerLimit.1")} <b>${CurrencyUtils.format(
            taxableProfitIncludingDistributions
          )}</b> ${messages("fullResultsPage.notEligibleBelowLowerLimit.2")} <b>${CurrencyUtils.format(
            marginalRate.adjustedLowerThreshold
          )}</b>""")
      )
    } else {
      template(
        firstThreeSteps,
        Some(
          s"""${messages("fullResultsPage.notEligibleAboveUpperLimit.1")} <b>${CurrencyUtils.format(
              taxableProfitIncludingDistributions
            )}</b> ${messages("fullResultsPage.notEligibleAboveUpperLimit.2")} <b>${CurrencyUtils.format(
              marginalRate.adjustedUpperThreshold
            )}</b>"""
        )
      )
    }

  }

  private def taxableProfitTable(calculatorResult: CalculatorResult, taxableProfit: Int, distributions: Int)(implicit
    messages: Messages
  ): Html = {

    def table(d1: TaxDetails, d2: TaxDetails) = {
      val totalDays = d1.days + d2.days
      val taxProfitDistributions = taxableProfit + distributions
      govukTable(
        Table(
          rows = Seq(
            Seq(
              TableRow(content = HtmlContent(p(messages("fullResultsPage.taxableProfit.daysAllocated")))),
              TableRow(content = HtmlContent(d1.days.toString), classes = "govuk-table__cell--numeric"),
              TableRow(content = HtmlContent(d2.days.toString), classes = "govuk-table__cell--numeric"),
              TableRow(content = HtmlContent(totalDays.toString), classes = "govuk-table__cell--numeric")
            ),
            Seq(
              TableRow(content = HtmlContent(p(messages("fullResultsPage.taxableProfit")))),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(d1.adjustedProfit)),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(BigDecimal(d2.adjustedProfit))),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(taxableProfit)),
                classes = "govuk-table__cell--numeric"
              )
            ),
            Seq(
              TableRow(content = HtmlContent(p(messages("fullResultsPage.taxableProfit.distributions")))),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(d1.adjustedDistributions)),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(d2.adjustedDistributions)),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(distributions)),
                classes = "govuk-table__cell--numeric"
              )
            ),
            Seq(
              TableRow(content = HtmlContent(p(messages("fullResultsPage.taxableProfit.profitAndDistributions")))),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(d1.adjustedAugmentedProfit)),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(d2.adjustedAugmentedProfit)),
                classes = "govuk-table__cell--numeric"
              ),
              TableRow(
                content = HtmlContent(CurrencyUtils.decimalFormat(taxProfitDistributions)),
                classes = "govuk-table__cell--numeric"
              )
            )
          ),
          head = Some(
            Seq(
              HeadCell(),
              HeadCell(
                content = Text(messages("site.from.to", d1.year.toString, (d1.year + 1).toString)),
                classes = "govuk-table__header--numeric"
              ),
              HeadCell(
                content = Text(messages("site.from.to", d2.year.toString, (d2.year + 1).toString)),
                classes = "govuk-table__header--numeric"
              ),
              HeadCell(content = Text(messages("fullResultsPage.total")), classes = "govuk-table__header--numeric")
            )
          ),
          caption = Some(messages("fullResultsPage.taxableProfit")),
          captionClasses = "govuk-table__caption--m"
        )
      )
    }

    calculatorResult.fold(single => HtmlFormat.empty)(dual =>
      dual.year1 -> dual.year2 match {
        case (d1: FlatRate, d2: MarginalRate) => table(d1, d2)
        case (d1: MarginalRate, d2: FlatRate) => table(d1, d2)
        case _                                => HtmlFormat.empty
      }
    )
  }
}
