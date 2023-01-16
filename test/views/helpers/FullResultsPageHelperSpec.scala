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

import utils.FormatUtils._
import base.SpecBase
import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import connectors.sharedmodel.{ DualResult, FYRatio, FlatRate, MarginalRate, MarginalReliefConfig, SingleResult }
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

      "when MarginalRate, should display full results table" in {
        val calculatorResult = SingleResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 0, 6, 7, 13, 8, 9, 10, FYRatio(10, 10)),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
          .htmlFormat shouldMatchTo """
                                      |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">
                                      |   fullResultsPage.forFinancialYear
                                      |</h3>
                                      |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£13</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£9</b></p>
                                      |<div class="app-table" role="region" aria-label="fullResultsPage.calculationTableCaption" tabindex="0">
                                      |<table class="govuk-table">
                                      |  <caption class="govuk-table__caption govuk-visually-hidden">
                                      |     fullResultsPage.calculationTableCaption
                                      |   </caption>
                                      |   <thead class="govuk-table__head">
                                      |   <tr class="govuk-table__row">
                                      |     <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">No header</span></td>
                                      |     <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">fullResultsPage.variables</span></td>
                                      |     <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
                                      |     <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
                                      |   </tr>
                                      |   </thead>
                                      |     <tbody class="govuk-table__body">
                                      |       <tr class="govuk-table__row">
                                      |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
                                      |         <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
                                      |         <td class="govuk-table__cell">£9</td>
                                      |       </tr>
                                      |       <tr class="govuk-table__row">
                                      |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
                                      |         <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural)</td>
                                      |         <td class="govuk-table__cell">£6</td>
                                      |       </tr>
                                      |       <tr class="govuk-table__row">
                                      |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
                                      |         <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
                                      |         <td class="govuk-table__cell">(£11 + £111) × (10 fullResultsPage.day.plural ÷ 10 fullResultsPage.day.plural)</td>
                                      |         <td class="govuk-table__cell">£13</td>
                                      |       </tr>
                                      |     </tbody>
                                      |   </table>
                                      |   </div>
                                      |""".stripMargin.htmlFormat
      }
    }

    "dual result" - {
      "when flat rates only, should throw exception" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 4, 5, 6),
          FlatRate(epoch.getYear + 1, 11, 22, 33, 44, 55, 66),
          1
        )
        val caught =
          intercept[RuntimeException] { // Result type: IndexOutOfBoundsException
            FullResultsPageHelper.displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
          }
        caught.getMessage mustBe "Both financial years are flat rate"
      }
      "when marginal rate only, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 1, 2, 3, 4, 0, 6, 7, 13, 8, 9, 10, FYRatio(10, 1020)),
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, 88, 99, 1010, FYRatio(1010, 1020)),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
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
            |    <div class="app-table" role="region" aria-label="fullResultsPage.calculationTableCaption" tabindex="0">
            |    <table class="govuk-table">
            |      <caption class="govuk-table__caption govuk-visually-hidden">
            |     fullResultsPage.calculationTableCaption
            |   </caption>
            |    <thead class="govuk-table__head">
            |      <tr class="govuk-table__row">
            |        <td  class="govuk-table__header not-header"><span class="govuk-visually-hidden">No header</span></td>
            |        <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">fullResultsPage.variables</span></td>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |      </tr>
            |    </thead>
            |    <tbody class="govuk-table__body">
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |        <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |        <td class="govuk-table__cell">£9</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |        <td class="govuk-table__cell">£11 × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£6</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |        <td class="govuk-table__cell">(£11 + £111) × (10 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£13</td>
            |      </tr>
            |    </tbody>
            |  </table>
            |  </div>
            |</div>
            |<div class="govuk-tabs__panel" id="year1970">
            |  <h2 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h2>
            |  <p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£99</b></p>
            |  <div class="app-table" role="region" aria-label="fullResultsPage.calculationTableCaption" tabindex="0">
            |  <table class="govuk-table">
            |    <caption class="govuk-table__caption govuk-visually-hidden">
            |     fullResultsPage.calculationTableCaption
            |   </caption>
            |    <thead class="govuk-table__head">
            |      <tr class="govuk-table__row">
            |        <td  class="govuk-table__header not-header"><span class="govuk-visually-hidden">No header</span></td>
            |        <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">fullResultsPage.variables</span></td>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |        <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |      </tr>
            |    </thead>
            |    <tbody class="govuk-table__body">
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |        <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |        <td class="govuk-table__cell">£99</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |        <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£66</td>
            |      </tr>
            |      <tr class="govuk-table__row">
            |        <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
            |        <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |        <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1020 fullResultsPage.day.plural)</td>
            |        <td class="govuk-table__cell">£143</td>
            |      </tr>
            |    </tbody>
            |  </table>
            |  </div>
            |</div>
            |</div>
            |""".stripMargin.htmlFormat
      }
      "when flat rate for year 1 and marginal rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          FlatRate(epoch.getYear, 1, 2, 3, 0, 3, 4),
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, -10, 0, 1010, FYRatio(1010, 1014)),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
          .htmlFormat shouldMatchTo
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£0</b></p>
            |<div class="app-table" role="region" aria-label="fullResultsPage.calculationTableCaption" tabindex="0">
            |<table class="govuk-table">
            |  <caption class="govuk-table__caption govuk-visually-hidden">
            |     fullResultsPage.calculationTableCaption
            |   </caption>
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <td  class="govuk-table__header not-header"><span class="govuk-visually-hidden">No header</span></td>
            |      <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">fullResultsPage.variables</span></td>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£0</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |  </tbody>
            |</table>
            |</div>
            |""".stripMargin.htmlFormat
      }
      "when marginal rate for year 1 and flat rate for year 2, should display results table" in {
        val calculatorResult = DualResult(
          MarginalRate(epoch.getYear, 11, 22, 33, 44, 0, 66, 77, 143, -10, 0, 1010, FYRatio(1010, 1014)),
          FlatRate(epoch.getYear, 1, 2, 3, 0, 3, 4),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
          .htmlFormat shouldMatchTo
          """
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.notEligibleAboveUpperLimit.1 <b>£143</b> fullResultsPage.notEligibleAboveUpperLimit.2 <b>£0</b></p>
            |<div class="app-table" role="region" aria-label="fullResultsPage.calculationTableCaption" tabindex="0">
            |<table class="govuk-table">
            |  <caption class="govuk-table__caption govuk-visually-hidden">
            |     fullResultsPage.calculationTableCaption
            |   </caption>
            |  <thead class="govuk-table__head">
            |    <tr class="govuk-table__row">
            |      <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">No header</span></td>
            |      <td class="govuk-table__header not-header"><span class="govuk-visually-hidden">fullResultsPage.variables</span></td>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.calculation</th>
            |      <th scope="col" class="govuk-table__header">fullResultsPage.result</th>
            |    </tr>
            |  </thead>
            |  <tbody class="govuk-table__body">
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.adjustedUpperLimit</td>
            |      <td class="govuk-table__cell">£250,000 fullResultsPage.upperLimit × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural) ÷ (1 fullResultsPage.associatedCompany.singular + fullResultsPage.oneOriginalCompany)</td>
            |      <td class="govuk-table__cell">£0</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfit</td>
            |      <td class="govuk-table__cell">£11 × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£66</td>
            |    </tr>
            |    <tr class="govuk-table__row">
            |      <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
            |      <td class="govuk-table__cell">fullResultsPage.financialYear.taxableProfitDistributions</td>
            |      <td class="govuk-table__cell">(£11 + £111) × (1010 fullResultsPage.day.plural ÷ 1014 fullResultsPage.day.plural)</td>
            |      <td class="govuk-table__cell">£143</td>
            |    </tr>
            |  </tbody>
            |</table>
            |</div>
            |<h3 class="govuk-heading-m" style="margin-bottom: 4px;">fullResultsPage.forFinancialYear</h3>
            |<p class="govuk-body">fullResultsPage.marginalReliefNotAvailable</p>
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
            days = 1010,
            FYRatio(1010, 1020)
          ),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
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
            days = 1010,
            FYRatio(1010, 1020)
          ),
          1
        )

        FullResultsPageHelper
          .displayFullCalculationResult(calculatorResult, Left(1), 11, 111, config)
          .toString
          .trimNewLines
          .contains("fullResultsPage.notEligibleBelowLowerLimit") mustBe true
      }
    }
    "renders marginal relief formula" in {
      FullResultsPageHelper.marginalReliefFormula(messages)
    }
    "shows marginal relief explanation if marginal relief > 0" in {
      val calculatorResult = SingleResult(
        MarginalRate(
          year = epoch.getYear,
          corporationTaxBeforeMR = 11,
          taxRateBeforeMR = 22,
          corporationTax = 33,
          taxRate = 44,
          marginalRelief = 1,
          adjustedProfit = 66,
          adjustedDistributions = 77,
          adjustedAugmentedProfit = 88,
          adjustedLowerThreshold = 100000,
          adjustedUpperThreshold = 1000000,
          days = 1010,
          FYRatio(1010, 1020)
        ),
        1
      )

      FullResultsPageHelper.showMarginalReliefExplanation(calculatorResult) mustBe true
    }

    "does not show marginal relief explanation if marginal relief <= 0" in {
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
          days = 1010,
          FYRatio(1010, 1020)
        ),
        1
      )
      FullResultsPageHelper.showMarginalReliefExplanation(calculatorResult) mustBe false
    }

    "does not show marginal relief explanation if marginal relief not available" in {
      val calculatorResult = SingleResult(
        FlatRate(
          year = epoch.getYear,
          corporationTax = 33,
          taxRate = 44,
          adjustedProfit = 66,
          adjustedDistributions = 77,
          adjustedAugmentedProfit = 88,
          days = 1010
        ),
        1
      )
      FullResultsPageHelper.showMarginalReliefExplanation(calculatorResult) mustBe false
    }
  }
}
