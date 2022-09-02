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

import base.SpecBase
import connectors.sharedmodel.{DualResult, FlatRate, MarginalRate, MarginalReliefConfig, SingleResult}
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
          FlatRate(epoch.getYear, 1, 2, 3, 4)
        )
        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            FullResultsPageHelper.displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          }
        caught.getMessage mustBe "Only flat rate year is available"
      }

      "when MarginalRate, should display full results table" in {
        val calculatorResult = SingleResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        )
        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines mustBe
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<table class="govuk-table">
            |   <thead class="govuk-table__head">
            |      <tr class="govuk-table__row">
            |         <th scope="col" class="govuk-table__header"></th>
            |         <th scope="col" class="govuk-table__header"></th>
            |         <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |         <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |      </tr>
            |   </thead>
            |   <tbody class="govuk-table__body">
            |      <tr class="govuk-table__row">
            |         <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.adjustUpperLimit</td>
            |         <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |         <td class="govuk-table__cell">£9</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |         <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |         <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |         <td class="govuk-table__cell">£6</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |         <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |         <td class="govuk-table__cell">£6 + £111 × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |         <td class="govuk-table__cell">£13</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |         <td class="govuk-table__cell govuk-!-font-weight-bold">4</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction.description</td>
            |         <td class="govuk-table__cell">3 ÷ 200</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |         <td class="govuk-table__cell govuk-!-font-weight-bold">5</td>
            |         <td class="govuk-table__cell">fullResultsPage.financialYear.fullCalculation</td>
            |         <td class="govuk-table__cell">(£9 - £13) × (£6 ÷ £13) × (3 ÷ 200)</td>
            |         <td class="govuk-table__cell">£5</td>
            |      </tr>
            |   </tbody>
            |</table>
            |""".stripMargin.trimNewLines
      }
    }

    "dual result" - {
      "when flat rates only, should throw exception" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 4), FlatRate(epoch.getYear + 1, 11, 22, 33, 44)
        )
        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            FullResultsPageHelper.displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          }
        caught.getMessage mustBe "Both financial years are flat rate"
      }
      "when marginal rate only, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), MarginalRate(epoch.getYear, 11, 22, 33, 44, 55, 66, 77, 88, 99, 1010)
        )
        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines mustBe
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
            |    <table class="govuk-table">
            |      <thead class="govuk-table__head">
            |        <tr class="govuk-table__row">
            |          <th scope="col" class="govuk-table__header"></th>
            |          <th scope="col" class="govuk-table__header"></th>
            |          <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |          <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |        </tr>
            |      </thead>
            |      <tbody class="govuk-table__body">
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.adjustUpperLimit</td>
            |          <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |          <td class="govuk-table__cell">£9</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |          <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |          <td class="govuk-table__cell">£6</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |          <td class="govuk-table__cell">£6 + £111 × (10 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |          <td class="govuk-table__cell">£13</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">4</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction.description</td>
            |          <td class="govuk-table__cell">3 ÷ 200</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">5</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.fullCalculation</td>
            |          <td class="govuk-table__cell">(£9 - £13) × (£6 ÷ £13) × (3 ÷ 200)</td>
            |          <td class="govuk-table__cell">£5</td>
            |        </tr>
            |      </tbody>
            |    </table>
            |  </div>
            |  <div class="govuk-tabs__panel" id="year1970">
            |    <h2 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h2>
            |    <table class="govuk-table">
            |      <thead class="govuk-table__head">
            |        <tr class="govuk-table__row">
            |          <th scope="col" class="govuk-table__header"></th>
            |          <th scope="col" class="govuk-table__header"></th>
            |          <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |          <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |        </tr>
            |      </thead>
            |      <tbody class="govuk-table__body">
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.adjustUpperLimit</td>
            |          <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |          <td class="govuk-table__cell">£99</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |          <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |          <td class="govuk-table__cell">£66</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |          <td class="govuk-table__cell">£66 + £111 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |          <td class="govuk-table__cell">£143</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">4</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction.description</td>
            |          <td class="govuk-table__cell">3 ÷ 200</td>
            |        </tr>
            |        <tr class="govuk-table__row">
            |          <td class="govuk-table__cell govuk-!-font-weight-bold">5</td>
            |          <td class="govuk-table__cell">fullResultsPage.financialYear.fullCalculation</td>
            |          <td class="govuk-table__cell">(£99 - £143) × (£66 ÷ £143) × (3 ÷ 200)</td>
            |          <td class="govuk-table__cell">£55</td>
            |        </tr>
            |      </tbody>
            |    </table>
            |  </div>
            |</div>
            |""".stripMargin.trimNewLines
      }
      "when flat rate for year 1 and marginal rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 4), MarginalRate(epoch.getYear, 11, 22, 33, 44, 55, 66, 77, 88, 99, 1010)
        )
        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines mustBe
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<table class="govuk-table">
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <th scope="col" class="govuk-table__header"></th>
            |      <th scope="col" class="govuk-table__header"></th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£99</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">£66 + £111 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">4</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction.description</td>
            |      <td class="govuk-table__cell">3 ÷ 200</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">5</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.fullCalculation</td>
            |      <td class="govuk-table__cell">(£99 - £143) × (£66 ÷ £143) × (3 ÷ 200)</td>
            |      <td class="govuk-table__cell">£55</td>
            |    </tr>
            |  </tbody>
            |</table>""".stripMargin.trimNewLines
      }
      "when marginal rate for year 1 and flat rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 55, 66, 77, 88, 99, 1010), FlatRate(epoch.getYear, 1, 2, 3, 4)
        )
        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, 1, 11, 111, config)
          .toString
          .trimNewLines mustBe
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<table class="govuk-table">
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <th scope="col" class="govuk-table__header"></th>
            |      <th scope="col" class="govuk-table__header"></th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">1</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£99</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">2</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">3</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">£66 + £111 × (1010 fullResultsPage.day.plural ÷ 365 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">4</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.marginalReliefFraction.description</td>
            |      <td class="govuk-table__cell">3 ÷ 200</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__cell govuk-!-font-weight-bold">5</td>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.fullCalculation</td>
            |      <td class="govuk-table__cell">(£99 - £143) × (£66 ÷ £143) × (3 ÷ 200)</td>
            |      <td class="govuk-table__cell">£55</td>
            |    </tr>
            |  </tbody>
            |</table>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>""".stripMargin.trimNewLines
      }
    }
  }
}
