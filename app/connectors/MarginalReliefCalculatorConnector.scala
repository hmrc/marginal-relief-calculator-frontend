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
import connectors.sharedmodel.{ AssociatedCompaniesParameter, CalculatorResult }
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, StringContextOps }

import java.time.LocalDate
import scala.concurrent.{ ExecutionContext, Future }

@ImplementedBy(classOf[MarginalReliefCalculatorConnectorImpl])
trait MarginalReliefCalculatorConnector {
  def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptDistributions: Option[Double],
    associatedCompanies: Option[Int],
    associatedCompaniesFY1: Option[Int],
    associatedCompaniesFY2: Option[Int]
  )(implicit hc: HeaderCarrier): Future[CalculatorResult]

  def associatedCompaniesParameters(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptDistributions: Option[Double]
  )(implicit hc: HeaderCarrier): Future[AssociatedCompaniesParameter]
}

class MarginalReliefCalculatorConnectorImpl @Inject() (httpClient: HttpClient, frontendAppConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends MarginalReliefCalculatorConnector {

  private val logger = LoggerFactory.getLogger(getClass)

  override def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptDistributions: Option[Double],
    associatedCompanies: Option[Int],
    associatedCompaniesFY1: Option[Int],
    associatedCompaniesFY2: Option[Int]
  )(implicit hc: HeaderCarrier): Future[CalculatorResult] = {
    logger.info("Calling marginal relief backend - /calculate")
    httpClient
      .GET[CalculatorResult](
        url"${frontendAppConfig.marginalReliefCalculatorUrl}/calculate?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&exemptDistributions=$exemptDistributions&associatedCompanies=$associatedCompanies&associatedCompaniesFY1=$associatedCompaniesFY1&associatedCompaniesFY2=$associatedCompaniesFY2"
      )
  }

  override def associatedCompaniesParameters(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptDistributions: Option[Double]
  )(implicit hc: HeaderCarrier): Future[AssociatedCompaniesParameter] = {
    logger.info("Calling marginal relief backend - /params/associated-companies")
    httpClient
      .GET[AssociatedCompaniesParameter](
        url"${frontendAppConfig.marginalReliefCalculatorUrl}/ask-params/associated-companies?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&exemptDistributions=$exemptDistributions"
      )
  }
}
