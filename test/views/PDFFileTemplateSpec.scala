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

package views

import base.SpecBase
import com.softwaremill.diffx.scalatest.DiffShouldMatcher.convertToAnyShouldMatcher
import models.calculator.{ DualResult, FYRatio, MarginalRate }
import models.MarginalReliefConfig
import forms.{ AccountingPeriodForm, PDFMetadataForm }
import org.jsoup.Jsoup
import play.api.test.Helpers.running
import forms.DateUtils
import views.html.PDFFileTemplate

import java.time.{ Instant, LocalDate }

class PDFFileTemplateSpec extends SpecBase {

  private val config = Map(
    2023 -> MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
  )
  "render" - {

    "should render dual result with marginal rates" in {

      implicit val now: Instant = Instant.now()
      val accountingPeriodForm =
        AccountingPeriodForm(LocalDate.parse("2023-04-01"), Some(LocalDate.parse("2024-03-31")))
      val application = applicationBuilder()
        .build()

      val calculatorResult = DualResult(
        MarginalRate(
          accountingPeriodForm.accountingPeriodStartDate.getYear,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          FYRatio(1, 2)
        ),
        MarginalRate(
          accountingPeriodForm.accountingPeriodStartDate.getYear,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          1,
          FYRatio(1, 2)
        ),
        1
      )

      running(application) {
        val StringUTR = "123456789012345"
        val view = application.injector.instanceOf[PDFFileTemplate]
        Jsoup
          .parse(
            view
              .render(
                Option(PDFMetadataForm(Some("company"), Some(StringUTR))),
                calculatorResult,
                accountingPeriodForm,
                1,
                1,
                Left(1),
                config,
                now,
                messages(application)
              )
              .toString
          )
          .html() shouldMatchTo expectation
      }
    }
  }

  private def expectation(implicit current: Instant): String = {
    s"""<html>
       | <head>
       |  <title>Marginal Relief for Corporation Tax results</title>
       |  <style>
       |         @page {
       |             size: A4;
       |             margin: 25px;
       |         }
       |         body {
       |             font-family:GDS Transport, arial, sans-serif;
       |             margin: 0px;
       |         }
       |         .pdf-container
       |         * {
       |            box-sizing: border-box;
       |        }
       |
       |        .grid-row {
       |            margin: 0px;
       |        }
       |
       |        .grid-row::after {
       |            clear: both;
       |            content: '';
       |            display: block;
       |        }
       |        .govuk-\\!-display-none, .govuk-visually-hidden {
       |            display: none!important;
       |        }
       |        .govuk-\\!-static-margin-bottom-1 {
       |            margin-bottom: 5px!important;
       |        }
       |        .sr-only {
       |            position: absolute;
       |            left: -10000px;
       |            top: auto;
       |            width: 1px;
       |            height: 1px;
       |            overflow: hidden;
       |        }
       |        .govuk-table__caption {
       |            font-weight: 700;
       |            display: table-caption;
       |            text-align: left;
       |        }
       |        .govuk-table {
       |             font-weight: 400;
       |             font-size: 1.1875rem;
       |             line-height: 1.31579;
       |             color: #0b0c0c;
       |             width: 100%;
       |             margin-bottom: 30px;
       |             border-spacing: 0;
       |             border-collapse: collapse;
       |         }
       |         .govuk-table__cell:last-child, .govuk-table__header:last-child {
       |            padding-right: 0;
       |         }
       |         .govuk-table__cell, .govuk-table__header {
       |             padding: 10px 20px 10px 0;
       |             border-bottom: 1px solid #b1b4b6;
       |             text-align: left;
       |             vertical-align: top;
       |         }
       |         .govuk-table__header {
       |            font-weight: 700;
       |         }
       |         .govuk-heading-m, .hmrc-timeline__event-title {
       |            font-size: 1.5rem;
       |            line-height: 1.25;
       |         }
       |         .govuk-grid-column-one-third {
       |            width: 33.3333%;
       |            float: left;
       |            box-sizing: border-box;
       |         }
       |        .govuk-grid-column-two-thirds {
       |            width: 66.6666%;
       |            float: left;
       |            box-sizing: border-box;
       |        }
       |         .govuk-grid-column-full {
       |             box-sizing: border-box;
       |             width: 100%;
       |             float: left;
       |         }
       |         .govuk-heading-s {
       |            color: #0b0c0c;
       |            font-weight: 700;
       |            font-size: 1.1875rem;
       |            line-height: 1.31579;
       |            display: block;
       |            margin-top: 0;
       |            margin-bottom: 15px;
       |        }
       |         .govuk-heading-l {
       |             color: #0b0c0c;
       |             font-weight: 700;
       |             font-size: 2.25rem;
       |             line-height: 1.11111;
       |             display: block;
       |             margin-top: 0;
       |             margin-bottom: 30px;
       |         }
       |         .govuk-body {
       |             color: #0b0c0c;
       |             font-weight: 400;
       |             font-size: 1.1875rem;
       |             line-height: 1.31579;
       |             margin-top: 0;
       |             margin-bottom: 20px;
       |         }
       |         .govuk-summary-list {
       |             font-weight: 400;
       |             font-size: 1.1875rem;
       |             line-height: 1.31579;
       |             color: #0b0c0c;
       |             margin: 0 0 20px;
       |             display: table;
       |             width: 100%;
       |             table-layout: fixed;
       |             border-collapse: collapse;
       |             margin-bottom: 30px;
       |         }
       |         .govuk-summary-list--no-border .govuk-summary-list__row {
       |            border: 0;
       |         }
       |         .govuk-summary-list__row {
       |            border-bottom: 1px solid #b1b4b6;
       |            display: table-row;
       |         }
       |         .govuk-summary-list__row:last-child {
       |            padding-right: 0;
       |         }
       |         .govuk-summary-list--no-border .govuk-summary-list--no-border .govuk-summary-list__key, .govuk-summary-list--no-border .govuk-summary-list__value {
       |            padding-bottom: 11px;
       |         }
       |         .govuk-summary-list__key {
       |             margin-bottom: 5px;
       |             font-weight: 700;
       |             width: 30%;
       |         }
       |         .govuk-summary-list__key, .govuk-summary-list__value {
       |             word-wrap: break-word;
       |             margin: 0;
       |             display: table-cell;
       |             padding-top: 10px;
       |             padding-right: 20px;
       |             padding-bottom: 10px;
       |         }
       |         .govuk-section-break--visible {
       |            border-bottom: 1px solid #b1b4b6;
       |         }
       |         .govuk-section-break--l {
       |             margin-top: 30px;
       |             margin-bottom: 30px;
       |         }
       |         .govuk-section-break {
       |             margin: 0;
       |             border: 0;
       |         }
       |
       |        .govuk-body-s {
       |            color: #0b0c0c;
       |            font-weight: 400;
       |            font-size: .875rem;
       |            line-height: 1.14286;
       |            margin-top: 0;
       |            margin-bottom: 15px;
       |        }
       |
       |        .govuk-padding-left-0 {
       |            padding-left: 0;
       |        }
       |
       |        .govuk-padding-right-0 {
       |            padding-right: 0;
       |        }
       |
       |        .govuk-margin-bottom-static-3,
       |        .govuk-static-margin-bottom-3 {
       |            margin-bottom: 15px!important;
       |        }
       |
       |        .govuk-margin-0 {
       |            margin: 0!important;
       |        }
       |
       |        .govuk-panel {
       |            font-weight: 400;
       |            font-size: 1rem;
       |            line-height: 1.25;
       |            box-sizing: border-box;
       |            margin-bottom: 15px;
       |            padding: 35px;
       |            border: 5px solid rgba(0,0,0,0);
       |            text-align: center;
       |        }
       |        .govuk-panel__title {
       |            font-size: 3rem;
       |            line-height: 1.04167;
       |            font-weight: 700;
       |        }
       |        .govuk-panel__body {
       |            font-weight: 400;
       |            font-size: 2.25rem;
       |            line-height: 1.11111;
       |        }
       |
       |        .pdf-page {
       |            page-break-after:always;
       |            position: relative;
       |            width: 100%;
       |            height: 283mm;
       |        }
       |
       |        .pdf-page .pdf-page-header .govuk-grid-column-one-third {
       |            border-left: #28a197 solid;
       |            padding-left: 8px;
       |        }
       |
       |        .pdf-page .pdf-page-header {
       |            margin-bottom:15px;
       |        }
       |        .pdf-page .pdf-page-header .govuk-grid-column-one-third {
       |            border-left:#28a197 solid;
       |            padding-left:8px;
       |            padding-right:20px;
       |        }
       |        .pdf-page .pdf-page-header span {
       |            display:block;
       |            font-size:1.4em;
       |            font-weight:normal;
       |            margin:0;
       |            line-height:1
       |        }
       |        .pdf-page .pdf-page-header h1 {
       |            text-align:right;
       |            margin:2em 0 0;
       |            color:#0b0c0c;
       |            font-weight:600
       |        }
       |        .pdf-page .pdf-banner .govuk-panel--confirmation {
       |            border:2px solid #28a197;
       |            display:block;
       |            margin-bottom:1em;
       |            padding:5px 10px;
       |            background-color:#e1f3f1;
       |        }
       |        .pdf-page .pdf-banner .govuk-panel--confirmation .govuk-panel__body {
       |            color:#0b0c0c;
       |            margin-top:0
       |        }
       |        .pdf-page .pdf-banner .govuk-panel--confirmation .govuk-panel__title {
       |            color:#0b0c0c
       |        }
       |
       |        .pdf-page hr {
       |            display:none
       |        }
       |        .pdf-page .about-results {
       |            background:#e1f3f1;
       |            padding:1px 10px;
       |            print-color-adjust:exact
       |        }
       |        .pdf-page .about-results h2 {
       |            margin-top:1.05263em;
       |            margin-bottom:0
       |        }
       |        .pdf-page .about-results .about-results-border {
       |            border-bottom:2px solid #28a197;
       |            margin-bottom:20px;
       |            line-height:1.3157894737;
       |            padding-bottom:10px
       |        }
       |        .pdf-page .govuk-details {
       |            display:none
       |        }
       |        .footer-page-no {
       |            position: absolute;
       |            bottom: 0px;
       |            left: 35px;
       |        }
       |    </style>
       | </head>
       | <body>
       |  <div class="pdf-container">
       |   <div class="pdf-page">
       |    <div class="grid-row pdf-page-header">
       |     <div class="govuk-grid-column-one-third">
       |      <img class="pdf-page-header__hmrc-logo" src="data:image/jpeg;base64,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" alt="HM Revenue &amp; Customs"> <span class="govuk-heading-m">HM Revenue &amp; Customs</span>
       |     </div>
       |     <div class="govuk-grid-column-two-thirds">
       |      <h1 class="govuk-heading-m pdf-page-header__heading">Marginal Relief for Corporation Tax results</h1>
       |     </div>
       |    </div>
       |    <div>
       |     <div class="grid-row">
       |      <h2 class="govuk-heading-s govuk-!-static-margin-bottom-1">Company name</h2>
       |      <p class="govuk-body">company</p>
       |     </div>
       |     <div class="grid-row">
       |      <h2 class="govuk-heading-s govuk-!-static-margin-bottom-1">Unique Taxpayer Reference</h2>
       |      <p class="govuk-body">123456789012345</p>
       |     </div>
       |    </div>
       |    <div class="grid-row pdf-banner">
       |     <div class="govuk-panel govuk-panel--confirmation">
       |      <h2 class="govuk-panel__body govuk-!-static-margin-bottom-3">Marginal Relief for your accounting period is</h2>
       |      <div class="govuk-panel__title govuk-!-margin-0">
       |       £2
       |      </div>
       |     </div>
       |    </div>
       |    <div class="grid-row">
       |     <div class="govuk-grid-column-two-thirds govuk-!-padding-left-0">
       |      <div class="grid-row">
       |       <div class="govuk-grid-column-full">
       |        <h2 class="govuk-heading-m" style="margin-bottom: 4px">Your details</h2>
       |        <dl class="govuk-summary-list govuk-summary-list--no-border">
       |         <div class="govuk-summary-list__row">
       |          <dt class="govuk-summary-list__key">
       |           Accounting period
       |          </dt>
       |          <dd class="govuk-summary-list__value">
       |           1 April 2023 to 31 March 2024
       |          </dd>
       |         </div>
       |         <div class="govuk-summary-list__row">
       |          <dt class="govuk-summary-list__key">
       |           Company's profit
       |          </dt>
       |          <dd class="govuk-summary-list__value">
       |           £1
       |          </dd>
       |         </div>
       |         <div class="govuk-summary-list__row">
       |          <dt class="govuk-summary-list__key">
       |           Distributions
       |          </dt>
       |          <dd class="govuk-summary-list__value">
       |           £1
       |          </dd>
       |         </div>
       |         <div class="govuk-summary-list__row">
       |          <dt class="govuk-summary-list__key">
       |           Associated companies
       |          </dt>
       |          <dd class="govuk-summary-list__value">
       |           1
       |          </dd>
       |         </div>
       |        </dl>
       |        <p class="govuk-heading-s">Your accounting period covers 2 HMRC financial years</p>
       |        <p class="govuk-body govuk-!-margin-0">2023 to 2024: 1 April 2023 to 1 April 2023</p>
       |        <p class="govuk-body">2023 to 2024: 2 April 2023 to 31 March 2024</p>
       |        <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
       |       </div>
       |      </div>
       |     </div>
       |     <div class="govuk-grid-column-one-third govuk-!-padding-right-0">
       |      <div class="about-results">
       |       <h2 class="govuk-heading-s about-results-border">About this result</h2>
       |       <h3 class="govuk-heading-xs">Date of result</h3>
       |       <p class="govuk-body about-results-border">${DateUtils.formatInstantUTC(current)}</p>
       |       <h3 class="govuk-heading-xs">HMRC legal declaration</h3>
       |       <p class="govuk-body">HM Revenue and Customs cannot be held liable for incorrect output from this calculator. Correct information can only result from this calculator if correct details are entered</p>
       |      </div>
       |     </div>
       |    </div><span class="govuk-body-s footer-page-no">Page 1 of 4</span>
       |   </div>
       |   <div class="pdf-page">
       |    <div class="grid-row">
       |     <h2 class="govuk-heading-m" style="margin-bottom: 7px;">Corporation Tax liability</h2><span class="govuk-heading-l" style="margin-bottom: 4px;">£2</span>
       |     <p class="govuk-body">Reduced from £2 after £2 Marginal Relief</p>
       |     <table class="govuk-table">
       |      <caption class="govuk-table__caption govuk-visually-hidden">
       |       Corporation Tax liability breakdown for your accounting period
       |      </caption>
       |      <thead class="govuk-table__head">
       |       <tr class="govuk-table__row">
       |        <td class="govuk-table__header"></td>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">2023 to 2024</th>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">2023 to 2024</th>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">Overall</th>
       |       </tr>
       |      </thead>
       |      <tbody class="govuk-table__body">
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Days allocated to each financial year</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1 <span class="sr-only">Days</span></td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1 <span class="sr-only">Days</span></td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">2 <span class="sr-only">Days</span></td>
       |       </tr>
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Corporation Tax liability before Marginal Relief</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£2</td>
       |       </tr>
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Marginal Relief</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">−£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">−£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">−£2</td>
       |       </tr>
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Corporation Tax liability after Marginal Relief</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£1</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">£2</td>
       |       </tr>
       |      </tbody>
       |     </table>
       |    </div>
       |    <div class="grid-row">
       |     <h2 class="govuk-heading-m" style="margin-bottom: 7px;">Effective tax rate</h2><span class="govuk-heading-l" style="margin-bottom: 4px;">1%</span>
       |     <p class="govuk-body">Reduced from 100% after Marginal Relief</p>
       |     <table class="govuk-table">
       |      <caption class="govuk-table__caption govuk-visually-hidden">
       |       Effective tax rate breakdown for your accounting period
       |      </caption>
       |      <thead class="govuk-table__head">
       |       <tr class="govuk-table__row">
       |        <td class="govuk-table__header"></td>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">2023 to 2024</th>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">2023 to 2024</th>
       |        <th scope="col" class="govuk-table__header govuk-table__header--numeric">Overall</th>
       |       </tr>
       |      </thead>
       |      <tbody class="govuk-table__body">
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Days allocated to financial year</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1 <span class="sr-only">Days</span></td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1 <span class="sr-only">Days</span></td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">2 <span class="sr-only">Days</span></td>
       |       </tr>
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Corporation Tax main rate before Marginal Relief</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1%</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1%</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">100%</td>
       |       </tr>
       |       <tr class="govuk-table__row">
       |        <th scope="row" class="govuk-table__header">Effective Corporation Tax rate after Marginal Relief</th>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1%</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1%</td>
       |        <td class="govuk-table__cell govuk-table__cell--numeric">1%</td>
       |       </tr>
       |      </tbody>
       |     </table>
       |    </div><span class="govuk-body-s footer-page-no">Page 2 of 4</span>
       |   </div>
       |   <div class="pdf-page">
       |    <div class="grid-row">
       |     <h2 class="govuk-heading-l">How it's calculated</h2>
       |     <p class="govuk-body">Marginal Relief for your accounting period</p>
       |     <h3 class="govuk-heading-m" style="margin-bottom: 4px;">For financial year 2023 to 2024 (1 days)</h3>
       |     <div class="app-table" role="region" aria-label="Your Marginal Relief calculation in detail." tabindex="0">
       |      <table class="govuk-table">
       |       <caption class="govuk-table__caption govuk-visually-hidden">
       |        Your Marginal Relief calculation in detail.
       |       </caption>
       |       <thead class="govuk-table__head">
       |        <tr class="govuk-table__row">
       |         <td class="govuk-table__header"><span aria-hidden="true">Step</span></td>
       |         <th scope="col" class="govuk-table__header">Calculation variables</th>
       |         <th scope="col" class="govuk-table__header">Calculation</th>
       |         <th scope="col" class="govuk-table__header">Result</th>
       |        </tr>
       |       </thead>
       |       <tbody class="govuk-table__body">
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
       |         <td class="govuk-table__cell">Adjusted lower limit</td>
       |         <td class="govuk-table__cell">£50,000 lower limit × (1 day ÷ 2 days) ÷ (1 associated company + 1 original company)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
       |         <td class="govuk-table__cell">Taxable profit</td>
       |         <td class="govuk-table__cell">£1 × (1 day ÷ 2 days)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
       |         <td class="govuk-table__cell">Taxable profit including distributions</td>
       |         <td class="govuk-table__cell">(£1 + £1) × (1 day ÷ 2 days)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
       |         <td class="govuk-table__cell">Marginal Relief fraction</td>
       |         <td class="govuk-table__cell">The set fraction used by HMRC to provide a gradual increase in rate</td>
       |         <td class="govuk-table__cell">3 ÷ 200</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
       |         <td class="govuk-table__cell">Full calculation</td>
       |         <td class="govuk-table__cell">(£1 - £1) × (£1 ÷ £1) × (3 ÷ 200)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |       </tbody>
       |      </table>
       |     </div>
       |    </div><span class="govuk-body-s footer-page-no">Page 3 of 4</span>
       |   </div>
       |   <div class="pdf-page">
       |    <div class="grid-row">
       |     <h3 class="govuk-heading-m" style="margin-bottom: 4px;">For financial year 2023 to 2024 (1 days)</h3>
       |     <div class="app-table" role="region" aria-label="Your Marginal Relief calculation in detail." tabindex="0">
       |      <table class="govuk-table">
       |       <caption class="govuk-table__caption govuk-visually-hidden">
       |        Your Marginal Relief calculation in detail.
       |       </caption>
       |       <thead class="govuk-table__head">
       |        <tr class="govuk-table__row">
       |         <td class="govuk-table__header"><span aria-hidden="true">Step</span></td>
       |         <th scope="col" class="govuk-table__header">Calculation variables</th>
       |         <th scope="col" class="govuk-table__header">Calculation</th>
       |         <th scope="col" class="govuk-table__header">Result</th>
       |        </tr>
       |       </thead>
       |       <tbody class="govuk-table__body">
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 1</span><span aria-hidden="true">1</span></th>
       |         <td class="govuk-table__cell">Adjusted lower limit</td>
       |         <td class="govuk-table__cell">£50,000 lower limit × (1 day ÷ 2 days) ÷ (1 associated company + 1 original company)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 2</span><span aria-hidden="true">2</span></th>
       |         <td class="govuk-table__cell">Taxable profit</td>
       |         <td class="govuk-table__cell">£1 × (1 day ÷ 2 days)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 3</span><span aria-hidden="true">3</span></th>
       |         <td class="govuk-table__cell">Taxable profit including distributions</td>
       |         <td class="govuk-table__cell">(£1 + £1) × (1 day ÷ 2 days)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 4</span><span aria-hidden="true">4</span></th>
       |         <td class="govuk-table__cell">Marginal Relief fraction</td>
       |         <td class="govuk-table__cell">The set fraction used by HMRC to provide a gradual increase in rate</td>
       |         <td class="govuk-table__cell">3 ÷ 200</td>
       |        </tr>
       |        <tr class="govuk-table__row">
       |         <th scope="row" class="govuk-table__header"><span class="sr-only">Step 5</span><span aria-hidden="true">5</span></th>
       |         <td class="govuk-table__cell">Full calculation</td>
       |         <td class="govuk-table__cell">(£1 - £1) × (£1 ÷ £1) × (3 ÷ 200)</td>
       |         <td class="govuk-table__cell">£1</td>
       |        </tr>
       |       </tbody>
       |      </table>
       |     </div>
       |     <h3 class="govuk-heading-s" style="margin-bottom: 4px;">Marginal Relief formula is:</h3>
       |     <p class="govuk-body">(Adjusted upper limit - taxable profit including distributions) × (taxable profit ÷ taxable profit including distributions) × (Marginal Relief fraction)</p>
       |     <hr class="govuk-section-break govuk-section-break--l govuk-section-break--visible">
       |     <h3 class="govuk-heading-s">What to do next</h3>
       |     <ul class="govuk-list govuk-list--bullet">
       |      <li>Complete your Corporation Tax return</li>
       |      <li>Pay your Corporation Tax no later than <b>1 January 2025</b>.</li>
       |     </ul>
       |    </div><span class="govuk-body-s footer-page-no">Page 4 of 4</span>
       |   </div>
       |  </div>
       | </body>
       |</html>""".stripMargin
  }
}
