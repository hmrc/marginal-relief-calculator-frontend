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
import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import connectors.sharedmodel.{ DualResult, FlatRate, MarginalRate, MarginalReliefConfig, SingleResult }
import forms.{ AccountingPeriodForm, PDFMetadataForm }
import play.api.i18n.Messages
import play.api.test.Helpers
import play.twirl.api.Html
import utils.FormatUtils.HtmlFormat
import views.helpers.FullResultsPageHelper.nonTabCalculationResultsTable
import views.helpers.PDFViewHelper.{ pdfCorporationTaxHtml, pdfDetailedCalculationHtml, pdfDetailedCalculationHtmlWithoutHeader, pdfHeaderHtml, pdfTableHtml }
import java.time.LocalDate
import scala.collection.immutable.Seq

class PDFViewHelperSpec extends SpecBase {
  private implicit val messages: Messages = Helpers.stubMessages()
  private val config = Map(
    2023 -> MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
  )

  private val accountingPeriodForm =
    AccountingPeriodForm(LocalDate.parse("2023-01-01"), Some(LocalDate.parse("2023-12-31")))

  private val pdfMetadataForm = PDFMetadataForm(Some("company"), Some("utr"))

  private val taxableProfit = 75000
  private val distributions = 0
  private val associatedCompanies = 0

  "display PDF Html" - {
    "when accounting period falls in a single year" - {
      "when flat rate" in {
        val flatRate = FlatRate(1970, 1, 2, 3, 4, 5, 6)
        val calculatorResult = SingleResult(flatRate, 1)
        val pageCount = "3"

        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
                ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
                ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
                ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(Seq(flatRate), associatedCompanies, taxableProfit, distributions, config),
              calculatorResult,
              pageCount
            )}""").htmlFormat
      }
      "when marginal rate" in {
        val marginalRate = MarginalRate(2023, 250, 25, 200, 20, 50, 1000, 10, 0, 100, 1500, 365)
        val calculatorResult = SingleResult(marginalRate, 1)
        val pageCount = "3"
        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
              ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
             ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
             ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(
                Seq(marginalRate),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              calculatorResult,
              pageCount
            )}""").htmlFormat
      }
    }
    "when accounting period spans 2 years" - {
      "when flat rate for both years" in {

        val flatRate1 = FlatRate(1970, 190, 19, 1000, 100, 0, 0)
        val flatRate2 = FlatRate(1971, 200, 20, 1000, 100, 0, 0)

        val calculatorResult =
          DualResult(flatRate1, flatRate2, 1)

        val pageCount = "3"

        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
                ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
                ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
                ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(
                Seq(flatRate1, flatRate2),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              calculatorResult,
              pageCount
            )}""").htmlFormat
      }
      "when marginal rate year 1 and flat rate for year 2" in {
        val flatRate = FlatRate(2022, 190, 19, 1000, 100, 0, 0)
        val marginalRate = MarginalRate(2023, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0)
        val calculatorResult = DualResult(
          marginalRate,
          flatRate,
          1
        )

        val pageCount = "3"

        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
                ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
                ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
                ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(
                Seq(marginalRate, flatRate),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              calculatorResult,
              pageCount
            )}""").htmlFormat
      }
      "when flat rate year 1 and marginal rate for year 2" in {
        val flatRate = FlatRate(2023, 190, 19, 1000, 100, 0, 0)
        val marginalRate = MarginalRate(2022, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0)
        val calculatorResult = DualResult(
          marginalRate,
          flatRate,
          1
        )

        val pageCount = "3"

        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
                ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
                ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
                ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(
                Seq(flatRate, marginalRate),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              calculatorResult,
              pageCount
            )}""").htmlFormat
      }
      "when marginal rate year 1 and marginal rate for year 2" in {
        val marginalRate1 = MarginalRate(2023, 250, 25, 200, 20, 50, 1000, 10, 100, 1500, 100, 0)
        val marginalRate2 = MarginalRate(2023, 300, 30, 250, 25, 50, 1000, 10, 100, 1500, 100, 0)
        val calculatorResult = DualResult(
          marginalRate1,
          marginalRate2,
          1
        )

        val pageCount = "4"

        pdfTableHtml(
          calculatorResult,
          associatedCompanies,
          taxableProfit,
          distributions,
          config,
          pdfMetadataForm,
          accountingPeriodForm
        ).htmlFormat shouldMatchTo
          Html(s"""
                ${pdfHeaderHtml(
              pageCount,
              pdfMetadataForm,
              calculatorResult,
              accountingPeriodForm,
              taxableProfit,
              distributions,
              associatedCompanies
            )}
                ${pdfCorporationTaxHtml(pageCount, calculatorResult)}
                ${pdfDetailedCalculationHtml(
              nonTabCalculationResultsTable(
                Seq(marginalRate1),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              calculatorResult,
              pageCount
            )}
        ${pdfDetailedCalculationHtmlWithoutHeader(
              nonTabCalculationResultsTable(
                Seq(marginalRate2),
                associatedCompanies,
                taxableProfit,
                distributions,
                config
              ),
              pageCount
            )}""").htmlFormat
      }
    }
  }
};
