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

import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import connectors.sharedmodel._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import utils.FormatUtils._
import views.helpers.PDFFileTemplateHelper.pdfHowItsCalculated

import java.time.LocalDate

class PDFFileTemplateHelperSpec extends AnyFreeSpec with Matchers {

  private val epoch = LocalDate.ofEpochDay(0)
  private implicit val messages: Messages = stubMessages()

  "pdfHowItsCalculated" - {
    "should render single result with flat rate" in {
      val config = Map(
        epoch.getYear -> FlatRateConfig(epoch.getYear, 0.19)
      )
      val calculatorResult = SingleResult(FlatRate(epoch.getYear, 1, 1, 1, 1, 1, 1), 1)
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldMatchTo
        """
          |<div class="pdf-page">
          |        <div class="grid-row">
          |          <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |          <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£0</h2>
          |          <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |          <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3><p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
          |        </div>
          |</div>
          |""".stripMargin.htmlFormat
    }

    "should render single result with marginal rate" in {
      val config = Map(
        epoch.getYear -> MarginalReliefConfig(epoch.getYear, 50000, 250000, 0.19, 0.25, 0.015)
      )
      val calculatorResult = SingleResult(MarginalRate(epoch.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), 1)
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldBe
        """
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |      <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£1</h2>
          |      <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <div class="app-table" role="region" aria-label="fullResultsPage.calculationTable.hidden" tabindex="0">
          |         <table summary="fullResultsPage.calculationTableSummary" class="govuk-table">
          |            <thead class="govuk-table__head">
          |               <tr class="govuk-table__row">
          |                  <td scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></td>
          |                  <th scope="col" class="govuk-table__header"  ><span class="govuk-!-display-none">fullResultsPage.variables</span></th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.calculation</th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.result</th>
          |               </tr>
          |            </thead>
          |            <tbody class="govuk-table__body">
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.adjustedLowerLimit</td>
          |                  <td class="govuk-table__cell"  >£50,000 fullResultsPage.lowerLimit × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfit</td>
          |                  <td class="govuk-table__cell"  >£1 × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfitDistributions</td>
          |                  <td class="govuk-table__cell"  >(£1 + £1) × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction</td>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction.description</td>
          |                  <td class="govuk-table__cell"  >3 ÷ 200</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.fullCalculation</td>
          |                  <td class="govuk-table__cell"  >(£1 - £1) × (£1 ÷ £1) × (3 ÷ 200)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |            </tbody>
          |         </table>
          |      </div>
          |   </div>
          |</div>
          |""".stripMargin.htmlFormat
    }

    "should render dual result with flat rate and marginal rate" in {
      val config = Map(
        epoch.getYear     -> FlatRateConfig(epoch.getYear, 0.19),
        epoch.getYear + 1 -> MarginalReliefConfig(epoch.getYear + 1, 50000, 250000, 0.19, 0.25, 0.015)
      )
      val calculatorResult = DualResult(
        FlatRate(epoch.getYear, 1, 1, 1, 1, 1, 1),
        MarginalRate(epoch.getYear + 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
        1
      )
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldBe
        """
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |      <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£2</h2>
          |      <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <div class="app-table" role="region" aria-label="fullResultsPage.calculationTable.hidden" tabindex="0">
          |         <table summary="fullResultsPage.calculationTableSummary" class="govuk-table">
          |            <thead class="govuk-table__head">
          |               <tr class="govuk-table__row">
          |                  <td scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></td>
          |                  <th scope="col" class="govuk-table__header"  ><span class="govuk-!-display-none">fullResultsPage.variables</span></th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.calculation</th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.result</th>
          |               </tr>
          |            </thead>
          |            <tbody class="govuk-table__body">
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.adjustedLowerLimit</td>
          |                  <td class="govuk-table__cell"  >£50,000 fullResultsPage.lowerLimit × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfit</td>
          |                  <td class="govuk-table__cell"  >£1 × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfitDistributions</td>
          |                  <td class="govuk-table__cell"  >(£1 + £1) × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction</td>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction.description</td>
          |                  <td class="govuk-table__cell"  >3 ÷ 200</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.fullCalculation</td>
          |                  <td class="govuk-table__cell"  >(£2 - £2) × (£2 ÷ £2) × (3 ÷ 200)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |            </tbody>
          |         </table>
          |      </div>
          |   </div>
          |</div>
          |""".stripMargin.htmlFormat
    }

    "should render dual result with flat rates" in {
      val config = Map(
        epoch.getYear     -> FlatRateConfig(epoch.getYear, 0.19),
        epoch.getYear + 1 -> FlatRateConfig(epoch.getYear + 1, 0.19)
      )
      val calculatorResult = DualResult(
        FlatRate(epoch.getYear, 1, 1, 1, 1, 1, 1),
        FlatRate(epoch.getYear + 1, 2, 2, 2, 2, 2, 2),
        1
      )
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldBe
        """
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |      <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£0</h2>
          |      <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
          |   </div>
          |</div>
          |""".stripMargin.htmlFormat
    }

    "should render dual result with marginal rate and flat rate" in {
      val config = Map(
        epoch.getYear     -> MarginalReliefConfig(epoch.getYear, 50000, 250000, 0.19, 0.25, 0.015),
        epoch.getYear + 1 -> FlatRateConfig(epoch.getYear + 1, 0.19)
      )
      val calculatorResult = DualResult(
        MarginalRate(epoch.getYear, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
        FlatRate(epoch.getYear + 1, 1, 1, 1, 1, 1, 1),
        1
      )
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldBe
        """
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |      <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£2</h2>
          |      <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <div class="app-table" role="region" aria-label="fullResultsPage.calculationTable.hidden" tabindex="0">
          |         <table summary="fullResultsPage.calculationTableSummary" class="govuk-table">
          |            <thead class="govuk-table__head">
          |               <tr class="govuk-table__row">
          |                  <td scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></td>
          |                  <th scope="col" class="govuk-table__header"  ><span class="govuk-!-display-none">fullResultsPage.variables</span></th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.calculation</th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.result</th>
          |               </tr>
          |            </thead>
          |            <tbody class="govuk-table__body">
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.adjustedLowerLimit</td>
          |                  <td class="govuk-table__cell"  >£50,000 fullResultsPage.lowerLimit × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfit</td>
          |                  <td class="govuk-table__cell"  >£1 × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfitDistributions</td>
          |                  <td class="govuk-table__cell"  >(£1 + £1) × (2 fullResultsPage.day.plural ÷ 3 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction</td>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction.description</td>
          |                  <td class="govuk-table__cell"  >3 ÷ 200</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.fullCalculation</td>
          |                  <td class="govuk-table__cell"  >(£2 - £2) × (£2 ÷ £2) × (3 ÷ 200)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |            </tbody>
          |         </table>
          |      </div>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
          |   </div>
          |</div>
          |""".stripMargin.htmlFormat
    }

    "should render dual result with marginal rates" in {
      val config = Map(
        epoch.getYear     -> MarginalReliefConfig(epoch.getYear, 50000, 250000, 0.19, 0.25, 0.015),
        epoch.getYear + 1 -> MarginalReliefConfig(epoch.getYear + 1, 50000, 250000, 0.19, 0.25, 0.015)
      )
      val calculatorResult = DualResult(
        MarginalRate(epoch.getYear, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        MarginalRate(epoch.getYear, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
        1
      )
      pdfHowItsCalculated(calculatorResult, 1, 1, 1, config).htmlFormat shouldBe
        """
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h2 class="govuk-heading-l">fullResultsPage.howItsCalculated</h2>
          |      <h2 class="govuk-heading-m" style="margin-bottom: 4px;">£3</h2>
          |      <p class="govuk-body">fullResultsPage.marginalReliefForAccountingPeriod</p>
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <div class="app-table" role="region" aria-label="fullResultsPage.calculationTable.hidden" tabindex="0">
          |         <table summary="fullResultsPage.calculationTableSummary" class="govuk-table">
          |            <thead class="govuk-table__head">
          |               <tr class="govuk-table__row">
          |                  <td scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></td>
          |                  <th scope="col" class="govuk-table__header"  ><span class="govuk-!-display-none">fullResultsPage.variables</span></th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.calculation</th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.result</th>
          |               </tr>
          |            </thead>
          |            <tbody class="govuk-table__body">
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.adjustedLowerLimit</td>
          |                  <td class="govuk-table__cell"  >£50,000 fullResultsPage.lowerLimit × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfit</td>
          |                  <td class="govuk-table__cell"  >£1 × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfitDistributions</td>
          |                  <td class="govuk-table__cell"  >(£1 + £1) × (1 fullResultsPage.day.singular ÷ 1 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction</td>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction.description</td>
          |                  <td class="govuk-table__cell"  >3 ÷ 200</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.fullCalculation</td>
          |                  <td class="govuk-table__cell"  >(£1 - £1) × (£1 ÷ £1) × (3 ÷ 200)</td>
          |                  <td class="govuk-table__cell"  >£1</td>
          |               </tr>
          |            </tbody>
          |         </table>
          |      </div>
          |   </div>
          |</div>
          |<div class="pdf-page">
          |   <div class="grid-row">
          |      <h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
          |      <div class="app-table" role="region" aria-label="fullResultsPage.calculationTable.hidden" tabindex="0">
          |         <table summary="fullResultsPage.calculationTableSummary" class="govuk-table">
          |            <thead class="govuk-table__head">
          |               <tr class="govuk-table__row">
          |                  <td scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></td>
          |                  <th scope="col" class="govuk-table__header"  ><span class="govuk-!-display-none">fullResultsPage.variables</span></th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.calculation</th>
          |                  <th scope="col" class="govuk-table__header"  >fullResultsPage.result</th>
          |               </tr>
          |            </thead>
          |            <tbody class="govuk-table__body">
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.adjustedLowerLimit</td>
          |                  <td class="govuk-table__cell"  >£50,000 fullResultsPage.lowerLimit × (2 fullResultsPage.day.plural ÷ 2 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfit</td>
          |                  <td class="govuk-table__cell"  >£1 × (2 fullResultsPage.day.plural ÷ 2 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.taxableProfitDistributions</td>
          |                  <td class="govuk-table__cell"  >(£1 + £1) × (2 fullResultsPage.day.plural ÷ 2 fullResultsPage.day.plural)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction</td>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.marginalReliefFraction.description</td>
          |                  <td class="govuk-table__cell"  >3 ÷ 200</td>
          |               </tr>
          |               <tr class="govuk-table__row">
          |                  <th scope="row" class="govuk-table__header"  ><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
          |                  <td class="govuk-table__cell"  >fullResultsPage.financialYear.fullCalculation</td>
          |                  <td class="govuk-table__cell"  >(£2 - £2) × (£2 ÷ £2) × (3 ÷ 200)</td>
          |                  <td class="govuk-table__cell"  >£2</td>
          |               </tr>
          |            </tbody>
          |         </table>
          |      </div>
          |   </div>
          |</div>
          |""".stripMargin.htmlFormat
    }
  }
}
