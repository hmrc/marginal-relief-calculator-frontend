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

package connectors

import com.google.inject.{ ImplementedBy, Inject }
import config.FrontendAppConfig
import connectors.sharedmodel.{ AssociatedCompaniesParameter, CalculatorConfig, CalculatorResult, FYConfig }
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, StringContextOps }

import java.time.LocalDate
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[CalculatorConfigConnectorImpl])
trait CalculatorConfigConnector {
  def get(implicit hc: HeaderCarrier): Future[CalculatorConfig]
  def getMap(implicit hc: HeaderCarrier): Future[Map[Int, FYConfig]]
}

class CalculatorConfigConnectorImpl @Inject() (httpClient: HttpClient, frontendAppConfig: FrontendAppConfig)(implicit
  ec: ExecutionContext
) extends CalculatorConfigConnector {

  private val logger = LoggerFactory.getLogger(getClass)

  override def get(implicit hc: HeaderCarrier): Future[CalculatorConfig] = {
    logger.info("Calling marginal relief backend - /config")
    httpClient
      .GET[CalculatorConfig](
        url"${frontendAppConfig.marginalReliefCalculatorUrl}/config"
      )
  }

  override def getMap(implicit hc: HeaderCarrier): Future[Map[Int, FYConfig]] =
    get.map { config =>
      config.fyConfigs.map(config => config.year -> config).toMap
    }
}
