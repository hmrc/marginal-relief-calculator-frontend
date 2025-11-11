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

import cats.data.Validated
import config.FrontendAppConfig
import models.calculator.CalculatorResult
import models.FYConfig
import play.api.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class CalculationConfigService @Inject() (appConfig: FrontendAppConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  def getConfig(year: Int): Future[FYConfig] = {
    logger.info(message = "Retrieving config from configuration file")

    appConfig.calculatorConfig.findFYConfig(year)(config.ConfigMissingError.apply) match {
      case Validated.Valid(config) => Future.successful(config)
      case Validated.Invalid(e) =>
        Future.failed(
          new RuntimeException(s"Configuration for year ${e.head.year} is missing.")
        )
    }
  }

  def getAllConfigs[U <: CalculatorResult](
    calculatorResult: U
  ): Future[Map[Int, FYConfig]] =
    calculatorResult.fold(single =>
      getConfig(single.taxDetails.year)
        .map(config => Map(single.taxDetails.year -> config))
    )(dual =>
      for {
        y1 <- getConfig(dual.year1TaxDetails.year)
        y2 <- getConfig(dual.year2TaxDetails.year)
      } yield Map(dual.year1TaxDetails.year -> y1, dual.year2TaxDetails.year -> y2)
    )
}
