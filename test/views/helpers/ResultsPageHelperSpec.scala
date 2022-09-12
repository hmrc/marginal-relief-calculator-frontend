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
import connectors.sharedmodel.{ DualResult, FlatRate, MarginalRate, SingleResult }
import forms.AccountingPeriodForm
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.{ GovukPanel, GovukTable }
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import views.helpers.ResultsPageHelper.{ displayBanner, displayCorporationTaxTable, displayEffectiveTaxTable, displayYourDetails }
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
        val calculatorResult = SingleResult(MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))

        displayYourDetails(
          calculatorResult,
          AccountingPeriodForm(
            epoch,
            Some(epoch.plusDays(1))
          ),
          1,
          11,
          111,
          true
        ).body.trimNewLines mustBe
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
            | <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin.trimNewLines
      }
    }

    "when accounting period spans multiple years and displayCoversFinancialYears is false" in {
      val calculatorResult =
        DualResult(MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))

      displayYourDetails(
        calculatorResult,
        AccountingPeriodForm(
          epoch,
          Some(epoch.plusDays(1))
        ),
        1,
        11,
        111,
        true
      ).body.trimNewLines mustBe
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
          |<hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin.trimNewLines
    }

    "when accounting period spans multiple years and displayCoversFinancialYears is true" in {
      val calculatorResult =
        DualResult(MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), MarginalRate(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))

      displayYourDetails(
        calculatorResult,
        AccountingPeriodForm(
          epoch,
          Some(epoch.plusDays(1))
        ),
        1,
        11,
        111,
        false
      ).body.trimNewLines mustBe
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
          |<p class="govuk-body">site.from.to: site.from.to<br/>site.from.to: site.from.to</p>
          |<hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible"/>""".stripMargin.trimNewLines
    }
  }

  "displayBanner" - {
    "when accounting period falls in a single year" - {
      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.marginalReliefNotApplicable"))
          )
        )
      }

      "when marginal rate and profits are within thresholds" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 365))
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£50")
          )
        )
      }

      "when marginal rate, profits are equal to lower threshold and distributions 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 0, 100, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate, profits are equal to lower threshold and distributions greater than 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 10, 110, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate and profits are below lower threshold and distributions 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 0, 200, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate and profits are below threshold and distributions greater than 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 19, 19, 19, 19, 0, 100, 10, 200, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate, profits are equal to upper threshold and distributions 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 0, 100, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate, profits are equal to upper threshold and distributions greater than 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 100, 1000, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate and profits are above upper threshold and distributions 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 0, 200, 900, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate and profits are above threshold and distributions greater than 0" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 200, 900, 365))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate is 0, but profits are between threshold" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 190, 19, 190, 19, 0, 1000, 10, 1000, 1500, 365))
        val result = intercept[UnsupportedOperationException] {
          displayBanner(calculatorResult)
        }
        result.getMessage mustBe "Marginal relief was 0, but augmented profit was neither <= lower-threshold or >= upper-threshold. Probably a rounding issue!"
      }
    }

    "when accounting period spans 2 years" - {
      "when flat rate for both years" in {
        val calculatorResult = DualResult(FlatRate(1970, 190, 19, 1000, 100), FlatRate(1971, 200, 20, 1000, 100))
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.marginalReliefNotApplicable"))
          )
        )
      }
      "when flat rate year 1 and marginal rate for year 2" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 100),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£50")
          )
        )
      }

      "when marginal rate year 1 and flat rate for year 2" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100),
          FlatRate(1970, 190, 19, 1000, 100)
        )
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£50")
          )
        )
      }

      "when marginal rate for 2 years and MR for both years are positive" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£100")
          )
        )
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are below lower limits (no distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 0, 1000, 1500, 100),
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 0, 1000, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are below lower limits (with distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 10, 1100, 1500, 100),
          MarginalRate(1971, 190, 19, 190, 19, 0, 1000, 10, 1100, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsBelowMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are above upper limits (no distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 0, 100, 500, 100),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 0, 100, 500, 100)
        )
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate for 2 years, both years have 0 MR as adjusted profits are above upper limits (with distributions)" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 100, 500, 100),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 100, 500, 100)
        )
        displayBanner(calculatorResult) mustBe govukPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefNotEligible")),
            content = Text(messages("resultsPage.yourProfitsAndDistributionsAboveMarginalReliefLimit"))
          )
        )
      }

      "when marginal rate for 2 years, year 1 has positive MR and year 2 has 0 MR" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100),
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1100, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£50")
          )
        )
      }

      "when marginal rate for 2 years, year 1 has 0 MR and year 2 has positive MR" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1100, 1500, 100),
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100)
        )
        displayBanner(calculatorResult) mustBe bannerPanel(
          Panel(
            title = Text(messages("resultsPage.marginalReliefForAccPeriodIs")),
            content = Text("£50")
          )
        )
      }

      "when marginal rate for 2 years, year 1 has 0 MR as adjusted profits below lower threshold and year 2 has 0 MR as adjusted profits above upper threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 1100, 1500, 100),
          MarginalRate(1971, 300, 30, 200, 30, 0, 1000, 10, 100, 500, 100)
        )
        val result = intercept[UnsupportedOperationException] {
          displayBanner(calculatorResult)
        }
        result.getMessage mustBe "Marginal relief was 0, however adjusted profits for one year was below lower threshold and the other year was above upper threshold"
      }
    }
  }

  "displayCorporationTaxTable" - {

    "when accounting period falls in a single year" - {

      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4))
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("4"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                TableRow(content = Text("£1"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when marginal rate and profits are within thresholds" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 365))
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("365"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief"))),
                TableRow(content = Text("£250"))
              ),
              Seq(
                TableRow(content = Text(messages("site.marginalRelief"))),
                TableRow(content = Text("-£50"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                TableRow(content = Text("£200"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when marginal rate and profits are below lower threshold" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 500, 1500, 365))
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("365"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                TableRow(content = Text("£25"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
    }

    "when accounting period spans 2 years" - {
      "when flat rate for both years" in {
        val calculatorResult = DualResult(FlatRate(1970, 190, 19, 1000, 100), FlatRate(1971, 200, 20, 1000, 100))
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                TableRow(content = Text("£190")),
                TableRow(content = Text("£200")),
                TableRow(content = Text("£390"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when marginal rate for both years and profits are below lower threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 500, 1000, 100),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 10, 500, 1000, 100)
        )
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                TableRow(content = Text("£25")),
                TableRow(content = Text("£30")),
                TableRow(content = Text("£55"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when marginal rate for both years and profits are above upper threshold" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 250, 25, 0, 1000, 10, 100, 500, 100),
          MarginalRate(1971, 300, 30, 300, 30, 0, 1000, 10, 100, 500, 100)
        )
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiability"))),
                TableRow(content = Text("£250")),
                TableRow(content = Text("£300")),
                TableRow(content = Text("£550"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when marginal rate for both years and profits are within thresholds" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100)
        )
        displayCorporationTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToEachFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityBeforeMarginalRelief"))),
                TableRow(content = Text("£250")),
                TableRow(content = Text("£300")),
                TableRow(content = Text("£550"))
              ),
              Seq(
                TableRow(content = Text(messages("site.marginalRelief"))),
                TableRow(content = Text("-£50")),
                TableRow(content = Text("-£50")),
                TableRow(content = Text("-£100"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxLiabilityAfterMarginalRelief"))),
                TableRow(content = Text("£200")),
                TableRow(content = Text("£250")),
                TableRow(content = Text("£450"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
    }
  }

  "displayEffectiveTaxTable" - {
    "when accounting period falls in a single year" - {
      "when flat rate" in {
        val calculatorResult = SingleResult(FlatRate(1970, 1, 2, 3, 4))
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("4"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                TableRow(content = Text("2.00%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
      "when marginal rate" in {
        val calculatorResult = SingleResult(MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 1, 1100, 365))
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("365"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                TableRow(content = Text("25.00%"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                TableRow(content = Text("20.00%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
    }
    "when accounting period spans 2 years" - {
      "when flat rate for both years, display corporation tax main rate row" in {
        val calculatorResult = DualResult(FlatRate(1970, 190, 19, 1000, 100), FlatRate(1971, 200, 20, 1000, 100))
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRate"))),
                TableRow(content = Text("19.00%")),
                TableRow(content = Text("20.00%")),
                TableRow(content = Text("19.50%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
      "when marginal rate for both years and profits within MR thresholds, display corporation tax main rate before and effective tax rate after MR rows" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 250, 25, 200, 20, 50, 1000, 10, 100, 1100, 100),
          MarginalRate(1971, 300, 30, 250, 25, 50, 1000, 10, 100, 1100, 100)
        )
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("30.00%")),
                TableRow(content = Text("27.50%"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                TableRow(content = Text("20.00%")),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("22.50%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
      "when marginal rate for both years and profits below MR lower threshold, display small profit tax rate row and corporation tax before MR row is hidden" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 500, 1000, 100),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 10, 500, 1000, 100)
        )
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.smallProfitRate"))),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("30.00%")),
                TableRow(content = Text("27.50%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
      "when marginal rate for both years and profits above MR threshold, display effective corporation tax rate row and corporation tax before MR row is hidden" in {
        val calculatorResult = DualResult(
          MarginalRate(1970, 25, 25, 25, 25, 0, 100, 10, 10, 50, 100),
          MarginalRate(1971, 30, 30, 30, 30, 0, 100, 10, 10, 50, 100)
        )
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("30.00%")),
                TableRow(content = Text("27.50%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when flat rate for one year and marginal rate for another year and profits below MR threshold, display effective corporation tax rate row" in {
        val calculatorResult =
          DualResult(FlatRate(1970, 19, 19, 100, 100), MarginalRate(1971, 25, 25, 25, 25, 0, 100, 10, 500, 1000, 100))
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                TableRow(content = Text("19.00%")),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("22.00%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when flat rate for one year and marginal rate for another year and profits above MR threshold, display effective corporation tax rate row" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 100),
          MarginalRate(1971, 250, 25, 250, 25, 0, 1000, 10, 500, 1000, 100)
        )
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTax"))),
                TableRow(content = Text("19.00%")),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("22.00%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }

      "when flat rate for one year and marginal rate for another year and profits within MR threshold, display corporation tax rate before MR row and effective corporation tax rate after MR row" in {
        val calculatorResult = DualResult(
          FlatRate(1970, 190, 19, 1000, 100),
          MarginalRate(1971, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100)
        )
        displayEffectiveTaxTable(calculatorResult) mustBe govukTable(
          Table(
            head = Some(
              Seq(
                HeadCell(content = HtmlContent(s"""<span class="govuk-!-display-none">No header</span>""")),
                HeadCell(content = Text(messages("site.from.to", "1970", "1971"))),
                HeadCell(content = Text(messages("site.from.to", "1971", "1972"))),
                HeadCell(content = Text(messages("site.overall")))
              )
            ),
            rows = Seq(
              Seq(
                TableRow(content = Text(messages("resultsPage.daysAllocatedToFinancialYear"))),
                TableRow(content = Text("100")),
                TableRow(content = Text("100")),
                TableRow(content = Text("200"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.corporationTaxMainRateBeforeMarginalRelief"))),
                TableRow(content = Text("19.00%")),
                TableRow(content = Text("25.00%")),
                TableRow(content = Text("22.00%"))
              ),
              Seq(
                TableRow(content = Text(messages("resultsPage.effectiveCorporationTaxAfterMarginalRelief"))),
                TableRow(content = Text("19.00%")),
                TableRow(content = Text("20.00%")),
                TableRow(content = Text("19.50%"))
              )
            ),
            caption = None,
            firstCellIsHeader = true
          )
        )
      }
    }
  }
}
