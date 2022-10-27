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
import views.helpers.ResultsPageHelper.replaceTableHeader

import scala.collection.immutable.Seq

object FullResultsPageHelper extends ViewHelper {

  private val govukTable = new GovukTable()
  private val govukDetails = new GovukDetails()

  def nonTabCalculationResultsTable(
    taxDetails: Seq[TaxDetails],
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

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
            config
          )
        )
      }
    }

    HtmlFormat.fill(html)
  }

  def displayFullCalculationResult(
    calculatorResult: CalculatorResult,
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    def dualResultTable(dual: DualResult) = {
      def tabBtn(year: Int) =
        s"""<li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
           |      <a class="govuk-tabs__tab" href="#year$year">${messages(
            "site.from.to",
            year.toString,
            (year + 1).toString
          )}</a>
           |    </li>""".stripMargin

      def tabContent(marginalRate: MarginalRate) = {
        val year = marginalRate.year
        s"""<div class="govuk-tabs__panel" id="year$year">
           |    ${h2(
            text = messages(
              "fullResultsPage.forFinancialYear",
              year.toString,
              (year + 1).toString,
              marginalRate.days
            ),
            styles = "margin-bottom: 4px;"
          )}
           |    ${displayFullFinancialYearTable(
            marginalRate,
            associatedCompanies,
            taxableProfit,
            distributions,
            config
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
                |  ${marginalRates.map(rate => tabContent(rate)).mkString}
                |</div>
                |""".stripMargin)

      val daysInAccountingPeriod = dual.year1.days + dual.year2.days

      dual.year1 -> dual.year2 match {
        case (y1: MarginalRate, y2: MarginalRate) => tabDisplay(Seq(y1, y2), daysInAccountingPeriod)
        case (y1: MarginalRate, y2: FlatRate) =>
          nonTabCalculationResultsTable(
            Seq(y1, y2),
            associatedCompanies,
            taxableProfit,
            distributions,
            config
          )
        case (y1: FlatRate, y2: MarginalRate) =>
          nonTabCalculationResultsTable(
            Seq(y1, y2),
            associatedCompanies,
            taxableProfit,
            distributions,
            config
          )
        case _ => throw new RuntimeException("Both financial years are flat rate")
      }
    }

    calculatorResult.fold(single =>
      nonTabCalculationResultsTable(
        Seq(single.details),
        associatedCompanies,
        taxableProfit,
        distributions,
        config
      )
    )(dualResultTable)
  }

  def marginalReliefFormula(implicit messages: Messages): Html =
    Html(s"""<h3 class="govuk-heading-s" style="margin-bottom: 4px;">${messages(
             "fullResultsPage.marginalReliefFormula"
           )}</h3>
            |<p class="govuk-body">${messages("fullResultsPage.marginalReliefFormula.description")}</p>""".stripMargin)

  def showMarginalReliefExplanation(calculatorResult: CalculatorResult): Boolean = {
    val taxDetails = calculatorResult.fold(single => Seq(single.details))(dual => Seq(dual.year1, dual.year2))
    taxDetails.exists(taxDetails => taxDetails.fold(_ => false)(isFiveStepMarginalRate))
  }
  def whatIsMarginalRate(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    govukDetails(
      Details(
        summary = Text(messages("fullResultsPage.whatIsMarginalRateFraction")),
        content = HtmlContent(
          s"""<p>${messages("fullResultsPage.details.standardFraction")}</p>
             |    <p>${messages("fullResultsPage.details.standardFractionExample")}</p>
             |    <p><b>${messages("fullResultsPage.details.whatIsMarginalRate")}</b></p>
             |    <p>${messages("fullResultsPage.details.smallProfitRate")}
             |      <strong>${messages("fullResultsPage.details.smallProfitRateNumber")}</strong><br/>
             |        ${messages("fullResultsPage.details.mainProfitRate")}
             |       <strong> ${messages("fullResultsPage.details.mainProfitRateNumber")}</strong><br>
             |        ${messages("fullResultsPage.details.lowerLimitRate")}
             |        <strong>${messages("fullResultsPage.details.lowerLimitRateNumber")}</strong><br/>
             |        ${messages("fullResultsPage.details.upperLimitRate")}
             |        <strong>${messages("fullResultsPage.details.upperLimitRateNumber")}</strong></p>
             |    <p>
             |        ${messages("fullResultsPage.details.examples.1")}
             |       <strong> ${messages("fullResultsPage.details.examples.1.ans")}</strong><br/>
             |        ${messages("fullResultsPage.details.examples.2")}
             |        <strong>${messages("fullResultsPage.details.examples.2.ans")}</strong><br/>
             |        ${messages("fullResultsPage.details.examples.3")}
             |       <strong> ${messages("fullResultsPage.details.examples.3.ans")}</strong>
             |        ${messages("fullResultsPage.details.examples.3.rate")} <br/>
             |    </p>""".stripMargin
        )
      )
    )

  private def isFiveStepMarginalRate(marginalRate: MarginalRate) = marginalRate.marginalRelief > 0

  private def displayFullFinancialYearTable(
    marginalRate: MarginalRate,
    associatedCompanies: Int,
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    val yearConfig = config(marginalRate.year) match {
      case x: FlatRateConfig       => throw new RuntimeException("Configuration is flat where it should be marginal")
      case x: MarginalReliefConfig => x
    }

    def boldRow(text: String) = TableRow(
      content = HtmlContent(s"""<span class="sr-only">Step $text</span><span aria-hidden="true">$text</span>""")
    )

    val days = marginalRate.fyRatio.numerator.toInt

    val daysInAccountingPeriod = marginalRate.fyRatio.denominator

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
            HeadCell(
              content = HtmlContent(s"""<span class="govuk-visually-hidden">No header</span>"""),
              classes = "not-header"
            ),
            HeadCell(
              content =
                HtmlContent(s"""<span class="govuk-visually-hidden">${messages("fullResultsPage.variables")}</span>"""),
              classes = "not-header"
            ),
            HeadCell(content = Text(messages("fullResultsPage.calculation"))),
            HeadCell(content = Text(messages("fullResultsPage.result")))
          )
        ),
        firstCellIsHeader = true,
        caption = Some(messages("fullResultsPage.calculationTableCaption")),
        captionClasses = "govuk-visually-hidden"
      )
      description match {
        case Some(text) =>
          HtmlFormat.fill(
            Seq(
              p(text),
              Html(
                s"""<div class="app-table" role="region" aria-label="${messages(
                    "fullResultsPage.calculationTable.hidden"
                  )}" tabindex="0">""" + replaceTableHeader(
                  govukTable(table)
                ) + "</div >"
              )
            )
          )
        case _ =>
          Html(
            s"""<div class="app-table" role="region" aria-label="${messages(
                "fullResultsPage.calculationTable.hidden"
              )}" tabindex="0">""" + replaceTableHeader(
              govukTable(table)
            ) + "</div>"
          )
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
}
