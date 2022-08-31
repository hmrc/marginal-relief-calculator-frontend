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
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTable
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.{ CurrencyUtils, DecimalToFractionUtils }

import java.time.Year

object FullResultsPageHelper extends ViewHelper {

  private val govukTable = new GovukTable()

  def displayFullCalculationResult(
    calculatorResult: CalculatorResult,
    associatedCompanies: Int,
    taxableProfit: Long,
    distributions: Long,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    def nonTabDisplay(taxDetails: Seq[TaxDetails]) = {
      taxDetails match {
        case Seq(taxDetails: FlatRate) => throw new RuntimeException("Only flat rate year is available")
        case _                         => ()
      }
      val htmlString = taxDetails.map { td =>
        val year = td.year
        val days = td.days
        td.fold { flat =>
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
          ).mkString
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
           |    ${h2(
            messages(
              "fullResultsPage.forFinancialYear",
              year.toString,
              (year + 1).toString,
              marginalRate.days
            )
          )}
           |    ${displayFullFinancialYearTable(marginalRate, associatedCompanies, taxableProfit, distributions, config)}
           |  </div>""".stripMargin
      }

      def tabDisplay(marginalRates: Seq[MarginalRate]) =
        Html(s"""
                |<div class="govuk-tabs" data-module="govuk-tabs">
                |  <h2 class="govuk-tabs__title">
                |    ${messages("fullResultsPage.financialYearResults")}
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
            TableRow(content = Text(CurrencyUtils.format(ResultsPageHelper.marginalRelief(marginalRate))))
          )
        ),
        head = Some(
          Seq(
            HeadCell(),
            HeadCell(),
            HeadCell(content = Text(messages("fullResultsPage.calculation"))),
            HeadCell(content = Text(messages("fullResultsPage.result")))
          )
        )
      )
    )
  }
}
