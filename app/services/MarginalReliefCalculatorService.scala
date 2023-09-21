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

package services

import calculator.{ FlatRateCalculator, MarginalRateCalculator, MixedRateCalculator }
import cats.data.ValidatedNel
import cats.syntax.apply.catsSyntaxTuple2Semigroupal
import com.google.inject.{ Inject, Singleton }
import config.{ ConfigMissingError, FrontendAppConfig }
import models.calculator._
import models.{ FYConfig, FlatRateConfig, MarginalReliefConfig }
import utils.DateUtils

import java.time.LocalDate

@Singleton
class MarginalReliefCalculatorService @Inject() (appConfig: FrontendAppConfig) extends DateUtils {
  type ValidationResult[A] = ValidatedNel[ConfigMissingError, A]

  def compute(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: BigDecimal,
    distributions: BigDecimal,
    associatedCompanies: Option[Int],
    associatedCompaniesFY1: Option[Int],
    associatedCompaniesFY2: Option[Int]
  ): ValidationResult[CalculatorResult] = {

    def findConfig: Int => ValidatedNel[ConfigMissingError, FYConfig] =
      appConfig.calculatorConfig.findFYConfig(_)(ConfigMissingError)

    val fy1 = fyForDate(accountingPeriodStart)
    val fy2 = fyForDate(accountingPeriodEnd)

    val fyEndForAPStartDate: LocalDate = financialYearEnd(accountingPeriodStart)
    lazy val fy2Start: LocalDate = fyEndForAPStartDate.plusDays(1)

    val daysInAP: Int = daysBetweenInclusive(accountingPeriodStart, accountingPeriodEnd)

    val result: ValidatedNel[ConfigMissingError, CalculatorResult] = if (fy1 == fy2) {
      findConfig(fy1).map {
        case flatRateConfig: FlatRateConfig =>
          FlatRateCalculator.computeSingle(
            fyDataWrapper = FyDataWrapper.constructSingleYear[FlatRateConfig](
              fy1 = fy1,
              accountingPeriodStart = accountingPeriodStart,
              daysInAP = daysInAP,
              fyEndForAPStartDate = fyEndForAPStartDate,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFy1 = associatedCompaniesFY1,
              associatedCompaniesAp = associatedCompanies,
              fY1Config = flatRateConfig
            )
          )
        case marginalRateConfig: MarginalReliefConfig =>
          MarginalRateCalculator.computeSingle(
            fyDataWrapper = FyDataWrapper.constructSingleYear[MarginalReliefConfig](
              fy1 = fy1,
              accountingPeriodStart = accountingPeriodStart,
              daysInAP = daysInAP,
              fyEndForAPStartDate = fyEndForAPStartDate,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFy1 = associatedCompaniesFY1,
              associatedCompaniesAp = associatedCompanies,
              fY1Config = marginalRateConfig
            )
          )
      }
    } else {
      (findConfig(fy1), findConfig(fy2)).mapN {
        case (flatRateConfigYr1: FlatRateConfig, flatRateConfigYr2: FlatRateConfig) =>
          FlatRateCalculator.computeDouble(fyDataWrapper =
            FyDataWrapper.constructDualYear[FlatRateConfig](
              fy1 = fy1,
              fy2 = fy2,
              accountingPeriodStart = accountingPeriodStart,
              accountingPeriodEnd = accountingPeriodEnd,
              daysInAP = daysInAP,
              fyEndForAPStartDate = fyEndForAPStartDate,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFy1 = associatedCompaniesFY1,
              associatedCompaniesFy2 = associatedCompaniesFY2,
              associatedCompaniesAp = associatedCompanies,
              fY1Config = flatRateConfigYr1,
              fY2Config = flatRateConfigYr2
            )
          )

        case (marginalRateConfigYr1: MarginalReliefConfig, marginalRateConfigYr2: MarginalReliefConfig) =>
          MarginalRateCalculator.computeDouble(fyDataWrapper =
            FyDataWrapper.constructDualYear[MarginalReliefConfig](
              fy1 = fy1,
              fy2 = fy2,
              accountingPeriodStart = accountingPeriodStart,
              accountingPeriodEnd = accountingPeriodEnd,
              daysInAP = daysInAP,
              fyEndForAPStartDate = fyEndForAPStartDate,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFy1 = associatedCompaniesFY1,
              associatedCompaniesFy2 = associatedCompaniesFY2,
              associatedCompaniesAp = associatedCompanies,
              fY1Config = marginalRateConfigYr1,
              fY2Config = marginalRateConfigYr2
            )
          )

        case (marginalRateConfigYr1: MarginalReliefConfig, flatRateConfigYr2: FlatRateConfig) =>
          MixedRateCalculator.compute(
            fyDataWrapper = FyDataWrapper.constructMixedYear(
              fyFlat = fy2,
              fyMarginal = fy1,
              flatStart = fy2Start,
              flatEnd = accountingPeriodEnd,
              marginalStart = accountingPeriodStart,
              marginalEnd = fyEndForAPStartDate,
              daysInAP = daysInAP,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFlat = associatedCompaniesFY2,
              associatedCompaniesMarginal = associatedCompaniesFY1,
              associatedCompaniesAp = associatedCompanies,
              fYFlatConfig = flatRateConfigYr2,
              fYMarginalConfig = marginalRateConfigYr1
            )
          )

        case (flatRateConfigYr1: FlatRateConfig, marginalRateConfigYr2: MarginalReliefConfig) =>
          MixedRateCalculator.compute(fyDataWrapper =
            FyDataWrapper.constructMixedYear(
              fyFlat = fy1,
              fyMarginal = fy2,
              flatStart = accountingPeriodStart,
              flatEnd = fyEndForAPStartDate,
              marginalStart = fy2Start,
              marginalEnd = accountingPeriodEnd,
              daysInAP = daysInAP,
              profit = profit,
              distributions = distributions,
              associatedCompaniesFlat = associatedCompaniesFY1,
              associatedCompaniesMarginal = associatedCompaniesFY2,
              associatedCompaniesAp = associatedCompanies,
              fYFlatConfig = flatRateConfigYr1,
              fYMarginalConfig = marginalRateConfigYr2
            )
          )
      }
    }

    result.map(_.roundValsUp)
  }
}
