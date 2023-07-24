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

package providers

import cats.data.ValidatedNel
import cats.syntax.apply._
import config.{ConfigMissingError, FrontendAppConfig}
import connectors.MarginalReliefCalculatorConnector
import connectors.sharedmodel._
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, UnprocessableEntityException}
import utils.ShowCalculatorDisclaimerUtils.{DateOps, financialYearEnd}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AssociatedCompaniesParametersProvider @Inject() (connector: MarginalReliefCalculatorConnector,
                                                       appConfig: FrontendAppConfig) extends Logging {

  type ParameterConfigResult = ValidatedNel[ConfigMissingError, AssociatedCompaniesParameter]

  private def doGetParameters(accountingPeriodStart: LocalDate,
                              accountingPeriodEnd: LocalDate): ParameterConfigResult = {

    def findConfig: Int => ValidatedNel[ConfigMissingError, FYConfig] =
      appConfig.calculatorConfig.findFYConfig(_)(ConfigMissingError)

    val fyEndForAccountingPeriodStart: LocalDate = financialYearEnd(accountingPeriodStart)

    if (fyEndForAccountingPeriodStart.isEqualOrAfter(accountingPeriodEnd)) {
      val fy = fyEndForAccountingPeriodStart.minusYears(1).getYear
      val maybeFYConfig = findConfig(fy)

      maybeFYConfig.map {
        case _: FlatRateConfig => DontAsk
        case _: MarginalReliefConfig => AskFull
      }
    } else {
      val fy1 = fyEndForAccountingPeriodStart.minusYears(1).getYear
      val fy2 = fyEndForAccountingPeriodStart.getYear
      val maybeFY1Config = findConfig(fy1)
      val maybeFY2Config = findConfig(fy2)

      (maybeFY1Config, maybeFY2Config).mapN {
        case (_: FlatRateConfig, _: FlatRateConfig) => DontAsk
        case (c1: MarginalReliefConfig, c2: MarginalReliefConfig) =>
          if (c1.upperThreshold == c2.upperThreshold && c1.lowerThreshold == c2.lowerThreshold) {
            AskFull
          } else {
            AskBothParts(
              Period(accountingPeriodStart, fyEndForAccountingPeriodStart),
              Period(fyEndForAccountingPeriodStart.plusDays(1), accountingPeriodEnd)
            )
          }
        case (_: FlatRateConfig, _: MarginalReliefConfig) =>
          AskOnePart(Period(fyEndForAccountingPeriodStart.plusDays(1), accountingPeriodEnd))
        case (_: MarginalReliefConfig, _: FlatRateConfig) =>
          AskOnePart(Period(accountingPeriodStart, fyEndForAccountingPeriodStart))
      }
    }
  }

  def associatedCompaniesParameters(accountingPeriodStart: LocalDate, accountingPeriodEnd: LocalDate)
                                   (implicit hc: HeaderCarrier): Future[AssociatedCompaniesParameter] =
    if (appConfig.reworkEnabled) {
      logger.info(message = "Determining associated companies parameters locally")

      doGetParameters(accountingPeriodStart = accountingPeriodStart, accountingPeriodEnd = accountingPeriodEnd).fold(
        errors =>
          throw new UnprocessableEntityException(
            "Failed to determined associated company parameters for given data: " + errors
              .map { case ConfigMissingError(year) =>
                throw new UnprocessableEntityException(s"Configuration missing for financial year: $year")
              }
              .toList
              .mkString(", ")
          ),
        success => Future.successful(success)
      )

    } else {
      connector.associatedCompaniesParameters(
        accountingPeriodStart = accountingPeriodStart,
        accountingPeriodEnd = accountingPeriodEnd
      )
    }

}
