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
import connectors.sharedmodel.MarginalReliefResult
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, StringContextOps }

import java.time.LocalDate
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds
@ImplementedBy(classOf[MarginalReliefCalculatorConnectorImpl])
trait MarginalReliefCalculatorConnector[F[_]] {
  def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptionDistributions: Option[Double],
    associatedCompanies: Option[Int]
  )(implicit hc: HeaderCarrier): F[MarginalReliefResult]
}

class MarginalReliefCalculatorConnectorImpl @Inject() (httpClient: HttpClient, frontendAppConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends MarginalReliefCalculatorConnector[Future] {
  override def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptionDistributions: Option[Double],
    associatedCompanies: Option[Int]
  )(implicit hc: HeaderCarrier): Future[MarginalReliefResult] =
    httpClient
      .GET[MarginalReliefResult](
        url"${frontendAppConfig.marginalReliefCalculatorUrl}/calculate?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&exemptionDistributions=$exemptionDistributions&associatedCompanies=$associatedCompanies"
      )
}
