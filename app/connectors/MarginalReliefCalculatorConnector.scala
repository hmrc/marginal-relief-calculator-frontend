package connectors

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.sharedmodel.MarginalReliefResult
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait MarginalReliefCalculatorConnector[F[_]] {
  def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptionDistribution: Option[Double],
    associatedCompanies: Option[Double]
  )(implicit hc: HeaderCarrier): F[MarginalReliefResult]
}

@Singleton
class MarginalReliefCalculatorConnectorImpl @Inject() (httpClient: HttpClientV2, frontendAppConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends MarginalReliefCalculatorConnector[Future] {
  override def calculate(
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    profit: Double,
    exemptionDistribution: Option[Double],
    associatedCompanies: Option[Double]
  )(implicit hc: HeaderCarrier): Future[MarginalReliefResult] =
    httpClient
      .get(
        url"${frontendAppConfig.marginalReliefCalculatorUrl}/calculate?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&exemptionDistribution=$exemptionDistribution&associatedCompanies=$associatedCompanies"
      )
      .execute[MarginalReliefResult]
}
