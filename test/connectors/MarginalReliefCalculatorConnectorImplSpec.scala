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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.client.WireMock.{ verify => _, _ }
import com.github.tomakehurst.wiremock.client.{ ResponseDefinitionBuilder, WireMock }
import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import connectors.sharedmodel._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.test.{ HttpClientSupport, WireMockSupport }
import uk.gov.hmrc.http.{ GatewayTimeoutException, HeaderCarrier, HttpClient, UpstreamErrorResponse }

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ Duration, _ }

class MarginalReliefCalculatorConnectorImplSpec
    extends AnyFreeSpec with Matchers with WireMockSupport with MockitoSugar with ScalaFutures with IntegrationPatience
    with HttpClientSupport with TableDrivenPropertyChecks {

  implicit val as: ActorSystem = ActorSystem("MarginalReliefCalculatorConnectorImplSpec-as")

  trait Fixture {
    val accountingPeriodStart: LocalDate = LocalDate.ofEpochDay(0)
    val accountingPeriodEnd: LocalDate = LocalDate.ofEpochDay(0)
    val profit: Double = 1
    val exemptDistributions: Option[Double] = Some(1)
    val associatedCompanies: Option[Int] = Some(1)
    val associatedCompaniesFY1: Option[Int] = Some(2)
    val associatedCompaniesFY2: Option[Int] = Some(3)
    val mockHttpHook: HttpHook = mock[HttpHook](withSettings.lenient)
    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val connectionTimeout: String = "60 seconds"
    lazy val idleTimeout: String = "60 seconds"
    lazy val requestTimeout: String = "60 seconds"

    lazy val config: Configuration = Configuration(
      ConfigFactory
        .parseString(s"""
                        |microservice {
                        | services {
                        |   marginal-relief-calculator-backend {
                        |     host = $wireMockHost
                        |     port = $wireMockPort
                        |     path = ""
                        |   }
                        | }
                        |}
                        |
                        |play.ws.timeout.connection = $connectionTimeout
                        |play.ws.timeout.idle = $idleTimeout
                        |play.ws.timeout.request = $requestTimeout
                        |""".stripMargin)
        .withFallback(ConfigFactory.load())
    )
    lazy val frontendAppConfig: FrontendAppConfig = new FrontendAppConfig(config)

    lazy val httpClient: HttpClient = mkHttpClient(config.underlying)

    lazy val marginalReliefCalculatorConnector: MarginalReliefCalculatorConnectorImpl =
      new MarginalReliefCalculatorConnectorImpl(httpClient, frontendAppConfig)

    def stubCalculate(response: ResponseDefinitionBuilder): Unit =
      wireMockServer.stubFor(
        WireMock
          .get(
            s"/calculate?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit${exemptDistributions
                .map(
                  "&exemptDistributions" +
                    "=" + _
                )
                .getOrElse("")}${associatedCompanies.map("&associatedCompanies=" + _).getOrElse("")}${associatedCompaniesFY1
                .map("&associatedCompaniesFY1=" + _)
                .getOrElse("")}${associatedCompaniesFY2.map("&associatedCompaniesFY2=" + _).getOrElse("")}"
          )
          .willReturn(response)
      )
  }

  "MarginalReliefCalculatorConnectorImpl" - {

    "http errors" - {

      "should handle idle timeout" in new Fixture {
        override lazy val idleTimeout: String = "1 seconds"
        stubCalculate(
          aResponse()
            .withBody(Json.toJson(SingleResult(FlatRate(1, 1, 1, 1, 1, 1, 1)): CalculatorResult).toString())
            .withFixedDelay(((Duration(idleTimeout) + 1.seconds).toSeconds * 1000).toInt)
        )
        val result = marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodStart,
            accountingPeriodEnd,
            profit,
            exemptDistributions,
            associatedCompanies,
            associatedCompaniesFY1,
            associatedCompaniesFY2
          )
          .failed
          .futureValue
        result shouldBe a[GatewayTimeoutException]
      }

      "should handle request timeout" in new Fixture {
        override lazy val requestTimeout: String = "1 seconds"
        stubCalculate(
          aResponse()
            .withBody(Json.toJson(SingleResult(FlatRate(1, 1, 1, 1, 1, 1, 1)): CalculatorResult).toString())
            .withFixedDelay(((Duration(requestTimeout) + 1.seconds).toSeconds * 1000).toInt)
        )
        val result = marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodStart,
            accountingPeriodEnd,
            profit,
            exemptDistributions,
            associatedCompanies,
            associatedCompaniesFY1,
            associatedCompaniesFY2
          )
          .failed
          .futureValue
        result shouldBe a[GatewayTimeoutException]
      }

      "should handle service unavailable error" in new Fixture {
        stubCalculate(
          aResponse()
            .withStatus(StatusCodes.ServiceUnavailable.intValue)
            .withBody("Service is unavailable")
        )
        val result = marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodStart,
            accountingPeriodEnd,
            profit,
            exemptDistributions,
            associatedCompanies,
            associatedCompaniesFY1,
            associatedCompaniesFY2
          )
          .failed
          .futureValue
        result shouldBe a[UpstreamErrorResponse]
        result
          .asInstanceOf[UpstreamErrorResponse]
          .getMessage shouldBe "GET of 'http://localhost:6001/calculate?accountingPeriodStart=1970-01-01&accountingPeriodEnd=1970-01-01&profit=1.0&exemptDistributions=1.0&associatedCompanies=1&associatedCompaniesFY1=2&associatedCompaniesFY2=3' returned 503. Response body: 'Service is unavailable'"
        result.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe StatusCodes.ServiceUnavailable.intValue
      }
    }

    "calculate" - {
      "should return successful response" in new Fixture {
        val table = Table(
          "marginalReliefResult",
          SingleResult(FlatRate(1111, 1.0, 11.0, 111.0, 0, 1, 1)),
          DualResult(
            MarginalRate(
              1111, 1.0, 11.0, 111.0, 1111.0, 11111.0, 0, 0, 0, 0, 0, 0
            ),
            MarginalRate(
              2222, 2.0, 22.0, 222.0, 2222.0, 22222.0, 0, 0, 0, 0, 0, 0
            )
          )
        )

        forAll(table) { calculatorResult: CalculatorResult =>
          stubCalculate(
            aResponse()
              .withBody(Json.toJson(calculatorResult).toString())
          )

          val result: CalculatorResult = marginalReliefCalculatorConnector
            .calculate(
              accountingPeriodStart,
              accountingPeriodEnd,
              profit,
              exemptDistributions,
              associatedCompanies,
              associatedCompaniesFY1,
              associatedCompaniesFY2
            )
            .futureValue

          result shouldEqual calculatorResult
        }
      }
    }

    "associatedCompaniesParameters" - {

      "should return successful response" in new Fixture {

        val table = Table(
          "associatedCompaniesParameter",
          DontAsk,
          AskFull,
          AskOnePart(Period(accountingPeriodStart, accountingPeriodEnd)),
          AskBothParts(
            Period(accountingPeriodStart, accountingPeriodEnd),
            Period(accountingPeriodStart, accountingPeriodEnd)
          )
        )

        forAll(table) { (associatedCompaniesParameter: AssociatedCompaniesParameter) =>
          wireMockServer.stubFor(
            WireMock
              .get(
                s"/ask-params/associated-companies?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&${exemptDistributions
                    .map(
                      "exemptDistributions" +
                        "=" + _
                    )
                    .getOrElse("")}"
              )
              .willReturn(aResponse().withBody(Json.toJson(associatedCompaniesParameter).toString))
          )

          val result: AssociatedCompaniesParameter = marginalReliefCalculatorConnector
            .associatedCompaniesParameters(
              accountingPeriodStart,
              accountingPeriodEnd,
              profit,
              exemptDistributions
            )
            .futureValue

          result shouldEqual associatedCompaniesParameter
        }
      }
    }

    "config" - {
      "should return successful response" in new Fixture {
        wireMockServer.stubFor(
          WireMock
            .get(
              "/config/2023"
            )
            .willReturn(
              aResponse().withBody(
                Json
                  .toJson(
                    MarginalReliefConfig(2, 22, 222, 2222, 22222, 222222).asInstanceOf[FYConfig]
                  )
                  .toString
              )
            )
        )

        wireMockServer.stubFor(
          WireMock
            .get(
              "/config/2022"
            )
            .willReturn(
              aResponse().withBody(
                Json
                  .toJson(
                    FlatRateConfig(1, 1).asInstanceOf[FYConfig]
                  )
                  .toString
              )
            )
        )

        val result: FYConfig = marginalReliefCalculatorConnector.config(2023).futureValue
        val result2: FYConfig = marginalReliefCalculatorConnector.config(2022).futureValue

        result shouldEqual MarginalReliefConfig(2, 22, 222, 2222, 22222, 222222).asInstanceOf[FYConfig]
        result2 shouldEqual FlatRateConfig(1, 1)
      }
    }
  }
}
