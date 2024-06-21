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

import cats.data.ValidatedNel
import cats.syntax.apply._
import config.{ ConfigMissingError, FrontendAppConfig }
import models._
import models.associatedCompanies._
import play.api.Logging
import uk.gov.hmrc.http.UnprocessableEntityException
import utils.ShowCalculatorDisclaimerUtils.{ financialYearEnd, getFinancialYearForDate }

import java.time.LocalDate
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton
class AssociatedCompaniesParameterService @Inject() (appConfig: FrontendAppConfig) extends Logging {

  type ParameterConfigResult = ValidatedNel[ConfigMissingError, AssociatedCompaniesParameter]

  private def doGetParameters(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate
  ): ParameterConfigResult = {
    def findConfig: Int => ValidatedNel[ConfigMissingError, FYConfig] =
      appConfig.calculatorConfig.findFYConfig(_)(ConfigMissingError)

    val year1: Int = getFinancialYearForDate(accountingPeriodStart)
    val year2: Int = getFinancialYearForDate(accountingPeriodEnd)

    val fyr1Config: ValidatedNel[ConfigMissingError, FYConfig] = findConfig(year1)
    val fyr2Config: ValidatedNel[ConfigMissingError, FYConfig] = findConfig(year2)

    val fyEndForAccountingPeriodStart: LocalDate = financialYearEnd(accountingPeriodStart)

    def sameThresholds(config1: MarginalReliefConfig, config2: MarginalReliefConfig): Boolean =
      config1.upperThreshold == config2.upperThreshold && config1.lowerThreshold == config2.lowerThreshold

    (fyr1Config, fyr2Config).mapN {
      case (_: FlatRateConfig, _: FlatRateConfig)                                                             => DontAsk
      case (c1: MarginalReliefConfig, c2: MarginalReliefConfig) if sameThresholds(config1 = c1, config2 = c2) => AskFull
      case (_: MarginalReliefConfig, _: MarginalReliefConfig) =>
        AskBothParts(
          period1 = Period(start = accountingPeriodStart, end = fyEndForAccountingPeriodStart),
          period2 = Period(start = fyEndForAccountingPeriodStart.plusDays(1), end = accountingPeriodEnd)
        )
      case (_: FlatRateConfig, _: MarginalReliefConfig) =>
        AskOnePart(
          period = Period(start = fyEndForAccountingPeriodStart.plusDays(1), end = accountingPeriodEnd)
        )
      case (_: MarginalReliefConfig, _: FlatRateConfig) =>
        AskOnePart(
          period = Period(start = accountingPeriodStart, end = fyEndForAccountingPeriodStart)
        )
    }
  }

  def associatedCompaniesParameters(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate
  ): Future[AssociatedCompaniesParameter] = {
    logger.info(message = "Determining associated companies parameters locally")

    doGetParameters(accountingPeriodStart = accountingPeriodStart, accountingPeriodEnd = accountingPeriodEnd).fold(
      errors =>
        throw new UnprocessableEntityException(
          "Failed to determined associated company parameters for given data: " + errors
            .map { case ConfigMissingError(year) =>
              new UnprocessableEntityException(s"Configuration missing for financial year: $year")
            }
            .toList
            .mkString(", ")
        ),
      success => Future.successful(success)
    )

  }

}
