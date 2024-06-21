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

import config.{ ConfigMissingError, FrontendAppConfig }
import models.calculator.CalculatorResult
import play.api.Logging
import uk.gov.hmrc.http.UnprocessableEntityException

import java.time.LocalDate
import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton
class CalculatorService @Inject() (
  appConfig: FrontendAppConfig,
  calculator: MarginalReliefCalculatorService
) extends Logging {

  def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptDistributions: Option[Double],
    associatedCompanies: Option[Int],
    associatedCompaniesFY1: Option[Int],
    associatedCompaniesFY2: Option[Int]
  ): Future[CalculatorResult] = {
    logger.info(message = "Using reworked calculation solution")

    val distributions: BigDecimal = BigDecimal(exemptDistributions.getOrElse(0.0))

    val result: calculator.ValidationResult[CalculatorResult] = calculator.compute(
      accountingPeriodStart = accountingPeriodStart,
      accountingPeriodEnd = accountingPeriodEnd,
      profit = profit,
      distributions = distributions,
      associatedCompanies = associatedCompanies,
      associatedCompaniesFY1 = associatedCompaniesFY1,
      associatedCompaniesFY2 = associatedCompaniesFY2
    )

    result.fold(
      errors =>
        throw new UnprocessableEntityException(
          "Failed to calculate marginal relief: " + errors
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
