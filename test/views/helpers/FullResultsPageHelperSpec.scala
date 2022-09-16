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

import utils.FormatUtils._
import base.SpecBase
import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import connectors.sharedmodel.{ DualResult, FlatRate, MarginalRate, MarginalReliefConfig, SingleResult }
import play.api.i18n.Messages
import play.api.test.Helpers._

import java.time.LocalDate

class FullResultsPageHelperSpec extends SpecBase {
  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private val config = Map(
    epoch.getYear -> MarginalReliefConfig(epoch.getYear, 50000, 250000, 0.19, 0.25, 0.015)
  )
  private implicit val messages: Messages = stubMessages()

  "displayFullCalculationResult" - {

    "single result" - {

      "when only FlatRate, should throw exception" in {
        val calculatorResult = SingleResult(
          FlatRate(epoch.getYear, 1, 2, 3, 4, 5, 6)
        )
        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            FullResultsPageHelper.displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          }
        caught.getMessage mustBe "Only flat rate year is available"
      }

      "when MarginalRate, should display full results table" in {
        val calculatorResult = SingleResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 0, 6, 7, 13, 8, 9, 10)
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .htmlFormat shouldMatchTo """
                                      |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">
                                      |   fullResultsPage.forFinancialYear
                                      |</h3>
                                      |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£13</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£9</b></p>
                                      |<table class="govuk-table"><thead class="govuk-table__head">
                                      |   <tr class="govuk-table__row">
                                      |     <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
                                      |     <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
                                      |     <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
                                      |     <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
                                      |   </tr>
                                      |   </thead>
                                      |     <tbody class="govuk-table__body">
                                      |       <tr class="govuk-table__row">
                                      |         <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
                                      |         <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
                                      |         <td class="govuk-table__cell">£9</td>
                                      |       </tr>
                                      |       <tr class="govuk-table__row">
                                      |         <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
                                      |         <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural)</td>
                                      |         <td class="govuk-table__cell">£6</td>
                                      |       </tr>
                                      |       <tr class="govuk-table__row">
                                      |         <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
                                      |         <td class="govuk-table__cell">(£11 + £111) × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural)</td>
                                      |         <td class="govuk-table__cell">£13</td>
                                      |       </tr>
                                      |     </tbody>
                                      |   </table>
                                      |""".stripMargin.htmlFormat
      }
    }

    "dual result" - {
      "when flat rates only, should throw exception" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 4, 5, 6),
          FlatRate(epoch.getYear + 1, 11, 22, 33, 44, 55, 66)
        )
        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            FullResultsPageHelper.displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          }
        caught.getMessage mustBe "Both financial years are flat rate"
      }
      "when marginal rate only, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 0, 6, 7, 13, 8, 9, 10),
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, 88, 99, 1010)
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .htmlFormat shouldMatchTo
          """
            |<div class="govuk-tabs" data-module="govuk-tabs">
            |  <h2 class="govuk-tabs__title">fullResultsPage.financialYearResults</h2>
            |  <ul class="govuk-tabs__list">
            |    <li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
            |      <a class="govuk-tabs__tab" href="#year1970">site.from.to</a>
            |    </li>
            |    <li class="govuk-tabs__list-item govuk-tabs__list-item--selected">
            |      <a class="govuk-tabs__tab" href="#year1970">site.from.to</a>
            |    </li>
            |  </ul>
            |  <div class="govuk-tabs__panel" id="year1970">
            |    <h2 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h2>
            |    <p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£13</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£9</b></p>
            |    <table class="govuk-table">
            |    <thead class="govuk-table__head">
            |      <tr class="govuk-table__row">
            |        <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |        <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |      </tr>
            |    </thead>
            |    <tbody class="govuk-table__body">
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |        <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |        <td class="govuk-table__cell">£9</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |        <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£6</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |        <td class="govuk-table__cell">(£11 + £111) × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£13</td>
            |      </tr>
            |    </tbody>
            |  </table>
            |</div>
            |<div class="govuk-tabs__panel" id="year1970">
            |  <h2 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h2>
            |  <p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£99</b></p>
            |  <table class="govuk-table">
            |    <thead class="govuk-table__head">
            |      <tr class="govuk-table__row">
            |        <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |        <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |      </tr>
            |    </thead>
            |    <tbody class="govuk-table__body">
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |        <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |        <td class="govuk-table__cell">£99</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |        <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£66</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |        <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£143</td>
            |      </tr>
            |    </tbody>
            |  </table>
            |</div>
            |</div>
            |""".stripMargin.htmlFormat
      }
      "when flat rate for year 1 and marginal rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 0, 3, 4),
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, -10, 0, 1010)
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .htmlFormat shouldMatchTo
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£0</b></p>
            |<table class="govuk-table">
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |      <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£0</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |  </tbody>
            |</table>
            |<table class="govuk-table">
            |  <caption class="govuk-table__caption govuk-table__caption--m">fullResultsPage.taxableProfit</caption>
            |  <thead class="govuk-table__head"><tr class="govuk-table__row">
            |    <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">site.from.to</th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">site.from.to</th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">fullResultsPage.total</th>
            |  </tr></thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit.daysAllocated</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">4</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">1010</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">1014</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£3.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£66.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£11.00</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit.distributions</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£0.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£77.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£111.00</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell"><p class="govuk-body">fullResultsPage.taxableProfit.profitAndDistributions</p></td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£3.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£143.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£122.00</td>
            |    </tr>
            |  </tbody>
            |</table>
            |""".stripMargin.htmlFormat
      }
      "when marginal rate for year 1 and flat rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, -10, 0, 1010),
          FlatRate(epoch.getYear, 1, 2, 3, 0, 3, 4)
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .htmlFormat shouldMatchTo
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£0</b></p>
            |<table class="govuk-table">
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |      <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£0</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |  </tbody>
            |</table>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
            |<table class="govuk-table">
            |  <caption class="govuk-table__caption govuk-table__caption--m">fullResultsPage.taxableProfit</caption>
            |  <thead class="govuk-table__head"><tr class="govuk-table__row">
            |    <th scope="col" class="govuk-table__header not-header"><span class="govuk-!-display-none">No header</span></th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">site.from.to</th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">site.from.to</th>
            |    <th scope="col" class="govuk-table__header govuk-table__header--numeric">fullResultsPage.total</th>
            |  </tr></thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit.daysAllocated</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">1010</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">4</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">1014</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£66.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£3.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£11.00</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell">
            |        <p class="govuk-body">fullResultsPage.taxableProfit.distributions</p>
            |      </td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£77.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£0.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£111.00</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell"><p class="govuk-body">fullResultsPage.taxableProfit.profitAndDistributions</p></td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£143.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£3.00</td>
            |      <td class="govuk-table__cell govuk-table__cell--numeric">£122.00</td>
            |    </tr>
            |  </tbody>
            |</table>
            |""".stripMargin.htmlFormat
      }
      "when marginal relief > 0 show correct template" in {
        val calculatorResult = SingleResult(
          MarginalRate(
            year = epoch.getYear,
            corporationTaxBeforeMR = 11,
            taxRateBeforeMR = 22,
            corporationTax = 33,
            taxRate = 44,
            marginalRelief = 55,
            adjustedProfit = 66,
            adjustedDistributions = 77,
            adjustedAugmentedProfit = 88,
            adjustedLowerThreshold = -100000,
            adjustedUpperThreshold = 100000,
            days = 1010
          )
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines
          .contains("fullResultsPage.financialYear.fullCalculation") mustBe true
      }
      "when taxable profit with distributions <=  adjusted  lower threshold should show correct template" in {
        val calculatorResult = SingleResult(
          MarginalRate(
            year = epoch.getYear,
            corporationTaxBeforeMR = 11,
            taxRateBeforeMR = 22,
            corporationTax = 33,
            taxRate = 44,
            marginalRelief = 0,
            adjustedProfit = 66,
            adjustedDistributions = 77,
            adjustedAugmentedProfit = 88,
            adjustedLowerThreshold = 100000,
            adjustedUpperThreshold = 1000000,
            days = 1010
          )
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines
          .contains("fullResultsPage.notEligibleBelowLowerLimit") mustBe true
      }
    }
  }
}
