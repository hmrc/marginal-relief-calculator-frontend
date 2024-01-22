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

import base.SpecBase
import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import models.calculator.{ DualResult, FYRatio, FlatRate, MarginalRate, SingleResult }
import forms.AccountingPeriodForm
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.Aliases.{ HeadCell, Panel, Table }
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukPanel, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{ HtmlContent, Text }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.FormatUtils.{ HtmlFormat, StringFormat }
import views.helpers.ResultsPageHelper.{ displayBanner, displayCorporationTaxTable, displayEffectiveTaxTable, displayYourDetails, isFlatRateOnly, replaceTableHeader, screenReaderText }
import views.html.templates.BannerPanel

import java.time.LocalDate

class ResultsPageHelperSpec extends SpecBase {

  private implicit val messages: Messages = Helpers.stubMessages()
  private val govukTable = new GovukTable()
  private val govukPanel = new GovukPanel()
  private val bannerPanel = new BannerPanel()
  private val epoch = LocalDate.ofEpochDay(0)

  "displayYourDetails" - {
    "when accounting period falls in a single year" - {
      "should return valid summary" in {
        val calculatorResult = SingleResult(MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)), 1)

        displayYourDetails(
          calculatorResult,
          AccountingPeriodForm(
            epoch,
            Some(epoch.plusDays(1))
          ),
          1,
          11,
          Left(111),
          true,
          false
        ).htmlFormat shouldMatchTo
          Jsoup
            .parse(
              """<h2 class="govuk-heading-m" style="margin-bottom: 4px">resultsPage.yourDetails</h2>
                | <dl class="govuk-summary-list govuk-summary-list--no-border">
                |   <div class="govuk-summary-list__row">
                |     <dt class="govuk-summary-list__key">resultsPage.accountPeriod</dt>
                |     <dd class="govuk-summary-list__value">site.from.to</dd>
                |   </div>
                |   <div class="govuk-summary-list__row">
                |     <dt class="govuk-summary-list__key">resultsPage.companysProfit</dt>
                |     <dd class="govuk-summary-list__value">£1</dd>
                |   </div>
                |   <div class="govuk-summary-list__row">
                |     <dt class="govuk-summary-list__key">resultsPage.distributions</dt>
                |     <dd class="govuk-summary-list__value">£11</dd>
                |   </div>
                |   <div class="govuk-summary-list__row">
                |     <dt class="govuk-summary-list__key">resultsPage.associatedCompanies</dt>
                |     <dd class="govuk-summary-list__value">111</dd>
                |   </div>
                | </dl>
                | <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin
            )
            .body
            .html
      }
    }

    "when accounting period spans multiple years and displayCoversFinancialYears is false" in {
      val calculatorResult =
        DualResult(
          MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)),
          MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)),
          1
        )

      displayYourDetails(
        calculatorResult,
        AccountingPeriodForm(
          epoch,
          Some(epoch.plusDays(1))
        ),
        1,
        11,
        Left(111),
        true,
        true
      ).htmlFormat shouldMatchTo
        """<h2 class="govuk-heading-m" style="margin-bottom: 4px">resultsPage.yourDetails</h2>
          |<dl class="govuk-summary-list govuk-summary-list--no-border">
          | <div class="govuk-summary-list__row">
          |   <dt class="govuk-summary-list__key">resultsPage.accountPeriod</dt>
          |   <dd class="govuk-summary-list__value">
          |     <p class="govuk-body">site.from.to</p>
          |     <p class="govuk-body">resultsPage.covers2FinancialYears</p>
          |   </dd>
          | </div>
          | <div class="govuk-summary-list__row">
          |   <dt class="govuk-summary-list__key">resultsPage.companysProfit</dt>
          |   <dd class="govuk-summary-list__value">£1</dd>
          | </div>
          | <div class="govuk-summary-list__row">
          |   <dt class="govuk-summary-list__key">resultsPage.distributions</dt>
          |   <dd class="govuk-summary-list__value">£11</dd>
          | </div>
          | <div class="govuk-summary-list__row">
          |   <dt class="govuk-summary-list__key">resultsPage.associatedCompanies</dt>
          |   <dd class="govuk-summary-list__value">111</dd>
          | </div>
          |</dl>
          |<p class="govuk-body">resultsPage.calculationDisclaimer</p>
          |<hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin.htmlFormat
    }

    "when accounting period spans multiple years and displayCoversFinancialYears is true" in {
      val calculatorResult =
        DualResult(
          MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)),
          MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)),
          1
        )

      displayYourDetails(
        calculatorResult,
        AccountingPeriodForm(
          epoch,
          Some(epoch.plusDays(1))
        ),
        1,
        11,
        Left(111),
        false,
        false
      ).htmlFormat shouldMatchTo
        """<h2 class="govuk-heading-m" style="margin-bottom: 4px">resultsPage.yourDetails</h2>
          |<dl class="govuk-summary-list govuk-summary-list--no-border">
          |  <div class="govuk-summary-list__row">
          |    <dt class="govuk-summary-list__key">resultsPage.accountPeriod</dt>
          |    <dd class="govuk-summary-list__value">site.from.to</dd>
          |  </div>
          |  <div class="govuk-summary-list__row">
          |    <dt class="govuk-summary-list__key">resultsPage.companysProfit</dt>
          |    <dd class="govuk-summary-list__value">£1</dd>
          |  </div>
          |  <div class="govuk-summary-list__row">
          |    <dt class="govuk-summary-list__key">resultsPage.distributions</dt>
          |    <dd class="govuk-summary-list__value">£11</dd>
          |  </div>
          |  <div class="govuk-summary-list__row">
          |   <dt class="govuk-summary-list__key">resultsPage.associatedCompanies</dt>
          |   <dd class="govuk-summary-list__value">111</dd>
          |  </div>
          |</dl>
          |<p class="govuk-heading-s">resultsPage.2years.period.heading</p>
          |<p class="govuk-body govuk-!-margin-0">site.from.to: site.from.to</p>
          |<p class="govuk-body">site.from.to: site.from.to</p>
          |<hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin.htmlFormat
    }
  }

  "displayBanner" - {
    "when accounting period falls in a single year" - {
      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4, 5, 6), 1)
        displayBanner(calculatorResult).title shouldMatchTo
          messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              content = Text(messages("resultsPage.marginalReliefNotEligibleFlatRate"))
            )
          ).htmlFormat
      }

      "when marginal rate and profits are within thresholds" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 0, 100, 1500, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo
          messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£50")
            )
          ).htmlFormat
      }

      "when marginal rate, profits are equal to lower threshold and distributions 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 0, 100, 100, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate, profits are equal to lower threshold and distributions greater than 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 10, 0, 110, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate and profits are below lower threshold and distributions 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 0, 0, 200, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate and profits are below threshold and distributions greater than 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 10, 0, 200, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate, profits are equal to upper threshold and distributions 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 0, 1000, 100, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate, profits are equal to upper threshold and distributions greater than 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 1010, 100, 1000, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate and profits are above upper threshold and distributions 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 0, 1000, 200, 900, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate and profits are above threshold and distributions greater than 0" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 1010, 200, 900, 365, FYRatio(365, 365)), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate is 0, but profits are between threshold" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 1010, 1000, 1500, 365, FYRatio(365, 365)), 1)
        val result = intercept[UnsupportedOperationException] {
          displayBanner(calculatorResult)
        }
        result.getMessage shouldMatchTo "Marginal relief was 0, but augmented profit was neither <= lower-threshold or >= upper-threshold. Probably a rounding issue!"
      }
    }

    "when accounting period spans 2 years" - {
      "when flat rate for both years" in {
        val calculatorResult =
          DualResult(FlatRate(1970, 190, 19, 1000, 100, 0, 0), FlatRate(1971, 200, 20, 1000, 100, 0, 0), 1)
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              content = Text(messages("resultsPage.marginalReliefNotEligibleFlatRate"))
            )
          ).htmlFormat
      }
      "when flat rate year 1 and marginal rate for year 2" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 100, 0, 0),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£50")
            )
          ).htmlFormat
      }

      "when marginal rate year 1 and flat rate for year 2" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          FlatRate(1970, 190, 19, 1000, 100, 0, 0),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£50")
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years and MR for both years are positive" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£100")
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are below lower limits (no distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 0, 1000, 1000, 1500, 100, FYRatio(100, 365)),
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 0, 1000, 1000, 1500, 100, FYRatio(100, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are below lower limits (with distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 10, 1010, 1100, 1500, 100, FYRatio(100, 365)),
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 10, 1010, 1100, 1500, 100, FYRatio(100, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years are outside of threshold limits (no distributions)" - {
        val calculatorResult = DualResult(
          MarginalRate(
            year = 1971,
            corporationTaxBeforeMR = 190,
            taxRateBeforeMR = 19,
            corporationTax = 190,
            taxRate = 19,
            marginalRelief = 0,
            adjustedProfit = 1000,
            adjustedDistributions = 0,
            adjustedAugmentedProfit = 1000,
            adjustedLowerThreshold = 1100,
            adjustedUpperThreshold = 1500,
            days = 100,
            FYRatio(100, 365)
          ),
          MarginalRate(
            year = 1971,
            corporationTaxBeforeMR = 190,
            taxRateBeforeMR = 19,
            corporationTax = 190,
            taxRate = 19,
            marginalRelief = 0,
            adjustedProfit = 2000,
            adjustedDistributions = 0,
            adjustedAugmentedProfit = 2000,
            adjustedLowerThreshold = 1100,
            adjustedUpperThreshold = 1500,
            days = 100,
            FYRatio(100, 365)
          ),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAboveAndBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years are outside of threshold limits (with distributions)" - {
        val calculatorResult = DualResult(
          MarginalRate(
            year = 1971,
            corporationTaxBeforeMR = 190,
            taxRateBeforeMR = 19,
            corporationTax = 190,
            taxRate = 19,
            marginalRelief = 0,
            adjustedProfit = 1000,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 1001,
            adjustedLowerThreshold = 1100,
            adjustedUpperThreshold = 1500,
            days = 100,
            FYRatio(100, 365)
          ),
          MarginalRate(
            year = 1971,
            corporationTaxBeforeMR = 190,
            taxRateBeforeMR = 19,
            corporationTax = 190,
            taxRate = 19,
            marginalRelief = 0,
            adjustedProfit = 2000,
            adjustedDistributions = 1,
            adjustedAugmentedProfit = 2001,
            adjustedLowerThreshold = 1100,
            adjustedUpperThreshold = 1500,
            days = 100,
            FYRatio(100, 365)
          ),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveAndBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are above upper limits (no distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 0, 100, 500, 100, 0, FYRatio(0, 365)),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 0, 100, 500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are above upper limits (with distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 100, 500, 100, 0, FYRatio(0, 365)),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 100, 500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, year 1 has positive MR and year 2 has 0 MR" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1100, 1500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£50")
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, year 1 has 0 MR and year 2 has positive MR" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1100, 1500, 100, 0, FYRatio(0, 365)),
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100, 0, FYRatio(0, 365)),
          1
        )
        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefForAccPeriodIs")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          bannerPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
              content = Text("£50")
            )
          ).htmlFormat
      }

      "when marginal rate for 2 years, year 1 has 0 MR as adjusted profits below lower threshold and year 2 has 0 MR as adjusted profits above upper threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1010, 1100, 1500, 100, FYRatio(100, 365)),
          MarginalRate(1971, 300, 30, 200, 30, 0, 1000, 10, 1010, 100, 500, 100, FYRatio(100, 365)),
          1
        )

        displayBanner(calculatorResult).title shouldMatchTo messages("resultsPage.marginalReliefNotEligible")
        displayBanner(calculatorResult).html.htmlFormat shouldMatchTo
          govukPanel(
            Panel(
              title = Text(messages("resultsPage.marginalReliefNotEligible")),
              content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveAndBelowMarginalReliefLimit"))
            )
          ).htmlFormat
      }
    }
  }

  "displayCorporationTaxTable" - {

    "when accounting period falls in a single year" - {

      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4, 5, 6), 1)
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(content = HtmlContent(s"""6 ${screenReaderText()}"""))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                    TableRow(content = Text("£1.00"))
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when marginal rate and profits are within thresholds" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 0, 10, 100, 1500, 365, FYRatio(365, 365)), 1)
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(content = HtmlContent(s"""365 ${screenReaderText()}"""))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief"))),
                    TableRow(content = Text("£250.00"))
                  ),
                  Seq(
                    TableRow(content = Text(messages("site.marginalRelief"))),
                    TableRow(content = Text("−£50.00"))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                    TableRow(content = Text("£200.00"))
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when marginal rate and profits are below lower threshold" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 25, 25, 25, 25, 0, 100, 0, 10, 500, 1500, 365, FYRatio(365, 365)), 1)
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(content = HtmlContent(s"""365 ${screenReaderText()}"""))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                    TableRow(content = Text("£25.00"))
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
    }

    "when accounting period spans 2 years" - {
      "when flat rate for both years" in {
        val calculatorResult =
          DualResult(FlatRate(1970, 190, 19, 0, 0, 1000, 100), FlatRate(1971, 200, 20, 0, 0, 1000, 100), 1)
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                    TableRow(content = Text("£190"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£200"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£390"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when marginal rate for both years and profits are below lower threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 0, 10, 500, 1000, 100, FYRatio(100, 365)),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 0, 10, 500, 1000, 100, FYRatio(100, 365)),
          1
        )
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                    TableRow(content = Text("£25"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£30"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£55"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when marginal rate for both years and profits are above upper threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 250, 25, 0, 1000, 10, 1010, 100, 500, 100, FYRatio(100, 365)),
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1010, 100, 500, 100, FYRatio(100, 365)),
          1
        )
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                    TableRow(content = Text("£250"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£300"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£550"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when marginal rate for both years and profits are within thresholds" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 0, 10, 100, 1500, 100, FYRatio(100, 365)),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 0, 10, 100, 1500, 100, FYRatio(100, 365)),
          1
        )
        replaceTableHeader(
          displayCorporationTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief"))),
                    TableRow(content = Text("£250"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£300"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£550"), classes = "govuk-table__cell--numeric")
                  ),
                  Seq(
                    TableRow(content = Text(messages("site.marginalRelief"))),
                    TableRow(content = Text("−£50"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("−£50"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("−£100"), classes = "govuk-table__cell--numeric")
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                    TableRow(content = Text("£200"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£250"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("£450"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveCorporationTaxTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
    }
  }

  "displayEffectiveTaxTable" - {
    "when accounting period falls in a single year" - {
      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4, 5, 6), 1)
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(content = HtmlContent(s"""6 ${screenReaderText()}"""))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                    TableRow(content = Text("2%"))
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
      "when marginal rate" in {
        val calculatorResult =
          SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 1, 0, 1100, 365, FYRatio(365, 365)), 25)
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(content = HtmlContent(s"""365 ${screenReaderText()}"""))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                    TableRow(content = Text("25%"))
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                    TableRow(content = Text("20%"))
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
    }
    "when accounting period spans 2 years" - {
      "when flat rate for both years, display corporation tax main rate row" in {
        val calculatorResult =
          DualResult(FlatRate(1970, 190, 19, 1000, 0, 1000, 100), FlatRate(1971, 200, 20, 1000, 0, 1000, 100), 19.5)
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                    TableRow(content = Text("19%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("20%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("19.50%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
      "when marginal rate for both years and profits within MR thresholds, display corporation tax main rate before and effective tax rate after MR rows" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 1010, 100, 1100, 100, FYRatio(100, 365)),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 1010, 100, 1100, 100, FYRatio(100, 365)),
          22.50
        )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("30%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("27.50%"), classes = "govuk-table__cell--numeric")
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                    TableRow(content = Text("20%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("22.50%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
      "when marginal rate for both years and profits below MR lower threshold, display small profit tax rate row and corporation tax before MR row is hidden" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 110, 500, 1000, 100, FYRatio(100, 365)),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 10, 110, 500, 1000, 100, FYRatio(100, 365)),
          27.50
        )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.smallProfitRate"))),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("30%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("27.50%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
      "when marginal rate for both years and profits above MR threshold, display effective corporation tax rate row and corporation tax before MR row is hidden" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 110, 10, 50, 100, FYRatio(100, 365)),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 10, 110, 10, 50, 100, FYRatio(100, 365)),
          27.50
        )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("30%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("27.50%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when flat rate for one year and marginal rate for another year and profits below MR threshold, display effective corporation tax rate row" in {
        val calculatorResult =
          DualResult(
            FlatRate(1970, 19, 19, 100, 0, 100, 100),
            MarginalRate(1971, 25, 25, 25, 25, 0, 100, 10, 110, 500, 1000, 100, FYRatio(100, 365)),
            22
          )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                    TableRow(content = Text("19%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("22%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when flat rate for one year and marginal rate for another year and profits above MR threshold, display effective corporation tax rate row" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 0, 1000, 100),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 1010, 500, 1000, 100, FYRatio(100, 365)),
          22
        )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                    TableRow(content = Text("19%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("22%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }

      "when flat rate for one year and marginal rate for another year and profits within MR threshold, display corporation tax rate before MR row and effective corporation tax rate after MR row" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 0, 1000, 100),
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 1010, 100, 1500, 100, FYRatio(100, 365)),
          19.5
        )
        replaceTableHeader(
          displayEffectiveTaxTable(calculatorResult)
        ).htmlFormat shouldMatchTo
          replaceTableHeader(
            govukTable(
              Table(
                head = Some(
                  Seq(
                    HeadCell(
                      content = Text(messages("site.from.to", "1970", "1971")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(
                      content = Text(messages("site.from.to", "1971", "1972")),
                      classes = "govuk-table__header--numeric"
                    ),
                    HeadCell(content = Text(messages("site.overall")), classes = "govuk-table__header--numeric")
                  )
                ),
                rows = Seq(
                  Seq(
                    TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""100 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    ),
                    TableRow(
                      content = HtmlContent(s"""200 ${screenReaderText()}"""),
                      classes = "govuk-table__cell--numeric"
                    )
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                    TableRow(content = Text("19%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("25%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("22%"), classes = "govuk-table__cell--numeric")
                  ),
                  Seq(
                    TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                    TableRow(content = Text("19%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("20%"), classes = "govuk-table__cell--numeric"),
                    TableRow(content = Text("19.50%"), classes = "govuk-table__cell--numeric")
                  )
                ),
                caption = Some(messages("resultsPage.effectiveTaxRateTableCaption")),
                captionClasses = "govuk-visually-hidden",
                firstCellIsHeader = true
              )
            )
          ).htmlFormat
      }
    }
  }
  "isFlatRateOnly" - {
    "when calculator result is single result flat rate, return true" - {
      val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4, 5, 6), 1)
      isFlatRateOnly(calculatorResult) shouldMatchTo true
    }
    "when calculator result is dual result flat rate, return true" - {
      val calculatorResult =
        DualResult(FlatRate(1970, 190, 19, 1000, 100, 0, 0), FlatRate(1971, 200, 20, 1000, 100, 0, 0), 1)
      isFlatRateOnly(calculatorResult) shouldMatchTo true
    }
    "when calculator result is marginal rate, return false" - {
      val calculatorResult =
        SingleResult(MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, FYRatio(1, 1)), 1)
      isFlatRateOnly(calculatorResult) shouldMatchTo false
    }
  }
}
