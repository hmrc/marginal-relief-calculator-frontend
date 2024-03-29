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
//
import models.{ FYConfig, FlatRateConfig, MarginalReliefConfig }
import models.calculator._
import play.api.i18n.Messages
import play.twirl.api.{ Html, HtmlFormat }
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukDetails, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.{ CurrencyUtils, DecimalToFractionUtils }

object FullResultsPageHelper extends ViewHelper {

  private val govukTable = new GovukTable()
  private val govukDetails = new GovukDetails()

  def nonTabCalculationResultsTable(
    calculatorResult: CalculatorResult,
    taxDetailsWithAssociatedCompanies: Seq[(TaxDetails, Int)],
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig],
    isPDF: Boolean
  )(implicit messages: Messages): Html = {

    val html = taxDetailsWithAssociatedCompanies.flatMap { case (td, associatedCompanies) =>
      val year = td.year
      val days = td.days
      if (isPDF) {
        td.fold { _ =>
          Seq(
            h3(messages("fullResultsPage.forFinancialYear", year.toString, (year + 1).toString, days)),
            p(messages("fullResultsPage.marginalReliefNotAvailable", year.toString, (year + 1).toString))
          )
        } { marginal =>
          Seq(
            h3(messages("fullResultsPage.forFinancialYear", year.toString, (year + 1).toString, days)),
            displayFullFinancialYearTable(
              calculatorResult,
              marginal,
              associatedCompanies,
              taxableProfit,
              distributions,
              config
            )
          )
        }
      } else {
        td.fold { _ =>
          Seq(
            h3FullResultsPage(messages("fullResultsPage.forFinancialYear", year.toString, (year + 1).toString, days)),
            p(messages("fullResultsPage.marginalReliefNotAvailable", year.toString, (year + 1).toString))
          )
        } { marginal =>
          Seq(
            h3FullResultsPage(messages("fullResultsPage.forFinancialYear", year.toString, (year + 1).toString, days)),
            displayFullFinancialYearTable(
              calculatorResult,
              marginal,
              associatedCompanies,
              taxableProfit,
              distributions,
              config
            )
          )
        }
      }
    }

    HtmlFormat.fill(html)
  }

  def tabBtn(year: Int)(implicit messages: Messages): String =
    s"""<li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
       |      <a class="govuk-tabs__tab" href="#year$year">${messages(
        "site.from.to",
        year.toString,
        (year + 1).toString
      )}</a>
       |    </li>""".stripMargin

  def displayFullCalculationResult(
    calculatorResult: CalculatorResult,
    associatedCompanies: Either[Int, (Int, Int)],
    taxableProfit: Int,
    distributions: Int,
    config: Map[Int, FYConfig]
  )(implicit messages: Messages): Html = {

    def dualResultTable(dual: DualResult[_ <: TaxDetails, _ <: TaxDetails]) = {

      def tabContent(marginalRate: MarginalRate, associatedCompanies: Int) = {
        val year = marginalRate.year
        s"""<div class="govuk-tabs__panel" id="year$year">
           |    ${h3FullResultsPage(
            text = messages("fullResultsPage.forFinancialYear", year.toString, (year + 1).toString, marginalRate.days)
          )}
           |    ${displayFullFinancialYearTable(
            calculatorResult,
            marginalRate,
            associatedCompanies,
            taxableProfit,
            distributions,
            config
          )}
           |  </div>""".stripMargin
      }

      def tabDisplay(marginalRates: Seq[(MarginalRate, Int)], daysInAccountingPeriod: Int) =
        Html(s"""
                |<div class="govuk-tabs" data-module="govuk-tabs">
                |  <h2 class="govuk-tabs__title">
                |    ${messages("fullResultsPage.financialYearResults")}
                |  </h2>
                |  <ul class="govuk-tabs__list">
                |    ${marginalRates.map(_._1.year).map(tabBtn).mkString}
                |  </ul>
                |  ${marginalRates.map(rate => tabContent(rate._1, rate._2)).mkString}
                |</div>
                |""".stripMargin)

      val daysInAccountingPeriod = dual.year1TaxDetails.days + dual.year2TaxDetails.days

      dual.year1TaxDetails -> dual.year2TaxDetails match {
        case (y1: MarginalRate, y2: MarginalRate) =>
          tabDisplay(taxDetailsWithAssociatedCompanies(Seq(y1, y2), associatedCompanies), daysInAccountingPeriod)
        case (y1: MarginalRate, y2: FlatRate) =>
          nonTabCalculationResultsTable(
            calculatorResult,
            taxDetailsWithAssociatedCompanies(Seq(y1, y2), associatedCompanies),
            taxableProfit,
            distributions,
            config,
            isPDF = false
          )
        case (y1: FlatRate, y2: MarginalRate) =>
          nonTabCalculationResultsTable(
            calculatorResult,
            taxDetailsWithAssociatedCompanies(Seq(y1, y2), associatedCompanies),
            taxableProfit,
            distributions,
            config,
            isPDF = false
          )
        case _ => throw new RuntimeException("Both financial years are flat rate")
      }
    }

    calculatorResult.fold(single =>
      nonTabCalculationResultsTable(
        calculatorResult,
        taxDetailsWithAssociatedCompanies(Seq(single.taxDetails), associatedCompanies),
        taxableProfit,
        distributions,
        config,
        isPDF = false
      )
    )(dualResultTable)
  }

  def taxDetailsWithAssociatedCompanies[T <: TaxDetails](
    taxDetails: Seq[T],
    associatedCompanies: Either[Int, (Int, Int)]
  ): Seq[(T, Int)] =
    associatedCompanies match {
      case Left(associatedCompanies) => taxDetails.map(_ -> associatedCompanies)
      case Right((associatedCompanies1, associatedCompanies2)) =>
        (taxDetails.head, associatedCompanies1) +: taxDetails.tail.map(_ -> associatedCompanies2)
    }

  def marginalReliefFormula(implicit messages: Messages): Html =
    Html(s"""<h3 class="govuk-heading-s" style="margin-bottom: 4px;">${messages(
             "fullResultsPage.marginalReliefFormula"
           )}</h3>
            |<p class="govuk-body">${messages("fullResultsPage.marginalReliefFormula.description")}</p>""".stripMargin)

  def showMarginalReliefExplanation(calculatorResult: CalculatorResult): Boolean = {
    val taxDetails =
      calculatorResult.fold(single => Seq(single.taxDetails))(dual => Seq(dual.year1TaxDetails, dual.year2TaxDetails))
    taxDetails.exists(taxDetails => taxDetails.fold(_ => false)(isFiveStepMarginalRate))
  }
  def whatIsMarginalRate(calculatorResult: CalculatorResult)(implicit messages: Messages): Html =
    govukDetails(
      Details(
        summary = Text(messages("fullResultsPage.whatIsMarginalRateFraction")),
        content = HtmlContent(
          s"""<p class="">${messages("fullResultsPage.details.standardFraction")}</p>
             |    <p><b>${messages("fullResultsPage.details.whatIsMarginalRate")}</b></p>
             |    <p>${messages("fullResultsPage.details.1")}<br/>
             |       ${messages("fullResultsPage.details.2")}<br/>
             |       <i>${messages("fullResultsPage.details.3.less")}</i> ${messages("fullResultsPage.details.3")}<br/>
             |       <i>${messages("fullResultsPage.details.4.equals")}</i> ${messages("fullResultsPage.details.4")}
             |    <p>
             |        ${messages("fullResultsPage.details.5")}<br/>
             |        ${messages("fullResultsPage.details.6")}
             |    </p>""".stripMargin
        )
      )
    )

  private def isFiveStepMarginalRate(marginalRate: MarginalRate) = marginalRate.marginalRelief > 0

  private def replaceTableHeader(tableHtml: Html)(implicit
    messages: Messages
  ): Html = {
    val th = s"""<th scope="col" class="govuk-table__header"  >${messages("site.step")}</th>"""
    val td = s"""<td class="govuk-table__header"><span aria-hidden="true">${messages("site.step")}</span></td>"""
    Html(
      tableHtml
        .toString()
        .replaceAll("[\n\r]", "")
        .replace(th, td)
    )
  }

  private def displayFullFinancialYearTable(
    calculatorResult: CalculatorResult,
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
      content = HtmlContent(
        s"""<span class="sr-only">${messages("site.step")} $text</span><span aria-hidden="true">$text</span>"""
      )
    )

    val days = marginalRate.fyRatio.numerator.toInt

    val daysInAP = calculatorResult.fold(_.taxDetails.days)(_.totalDays)
    val daysInAPForAdjustedUL = marginalRate.fyRatio.denominator

    val upperThreshold = CurrencyUtils.format(yearConfig.upperThreshold)

    val upperThresholdMsg = messages("fullResultsPage.upperLimit")

    val upperThresholdText = upperThreshold + " " + upperThresholdMsg

    val lowerThreshold = CurrencyUtils.format(yearConfig.lowerThreshold)

    val lowerThresholdMsg = messages("fullResultsPage.lowerLimit")

    val lowerThresholdText = lowerThreshold + " " + lowerThresholdMsg

    val dayMsg = messages("fullResultsPage.day.singular")
    val daysMsg = messages("fullResultsPage.day.plural")

    val daysString = days match {
      case 1 => s"$days $dayMsg"
      case _ => s"$days $daysMsg"
    }

    val associatedCompaniesText = associatedCompanies match {
      case 1 => s"1 ${messages("fullResultsPage.associatedCompany.singular")}"
      case x => s"$x ${messages("fullResultsPage.associatedCompany.plural")}"
    }

    val originalCompanyMsg = messages("fullResultsPage.oneOriginalCompany")

    val pointOneCompaniesCalcText = s"($associatedCompaniesText + $originalCompanyMsg)"

    val fraction = {
      val f = DecimalToFractionUtils.toFraction(yearConfig.marginalReliefFraction)
      s"${f.numerator} ÷ ${f.denominator}"
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
              else lowerThresholdText} × ($daysString ÷ $daysInAPForAdjustedUL $daysMsg) ÷ $pointOneCompaniesCalcText"
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
        TableRow(content = Text(s"${CurrencyUtils.format(taxableProfit)} × ($daysString ÷ $daysInAP $daysMsg)")),
        TableRow(content = Text(CurrencyUtils.format(marginalRate.adjustedProfit)))
      ),
      Seq(
        boldRow("3"),
        TableRow(content = Text(messages("fullResultsPage.financialYear.taxableProfitDistributions"))),
        TableRow(content =
          Text(
            s"(${CurrencyUtils.format(taxableProfit)} + ${CurrencyUtils
                .format(distributions)}) × ($daysString ÷ $daysInAP $daysMsg)"
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
            HeadCell(content = Text(messages("site.step"))),
            HeadCell(content = Text(messages("fullResultsPage.variables"))),
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
                    "fullResultsPage.calculationTableCaption"
                  )}" tabindex="0">""" + replaceTableHeader(
                  govukTable(table)
                ) + "</div >"
              )
            )
          )
        case _ =>
          Html(
            s"""<div class="app-table" role="region" aria-label="${messages(
                "fullResultsPage.calculationTableCaption"
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
