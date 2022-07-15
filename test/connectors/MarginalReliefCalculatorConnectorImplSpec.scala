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
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ verify => _, _ }
import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import connectors.sharedmodel.{ AskFull, AssociatedCompaniesParameter, MarginalReliefResult, SingleResult }
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.test.{ HttpClientSupport, WireMockSupport }

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

private class MarginalReliefCalculatorConnectorImplSpec
    extends AnyFreeSpec with Matchers with WireMockSupport with MockitoSugar with ScalaFutures with IntegrationPatience
    with HttpClientSupport {

  implicit val as: ActorSystem = ActorSystem("MarginalReliefCalculatorConnectorImplSpec-as")

  trait Fixture {
    val accountingPeriodStart: LocalDate = LocalDate.ofEpochDay(0)
    val accountingPeriodEnd: LocalDate = LocalDate.ofEpochDay(0)
    val profit: Double = 1
    val exemptDistributions: Option[Double] = Some(1)
    val associatedCompanies: Option[Int] = Some(1)
    val mockHttpHook: HttpHook = mock[HttpHook](withSettings.lenient)
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val config: Configuration = Configuration(
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
                        |""".stripMargin)
        .withFallback(ConfigFactory.load())
    )
    val frontendAppConfig: FrontendAppConfig = new FrontendAppConfig(config)

    val marginalReliefCalculatorConnector: MarginalReliefCalculatorConnectorImpl =
      new MarginalReliefCalculatorConnectorImpl(httpClient, frontendAppConfig)
  }

  "MarginalReliefCalculatorConnectorImpl" - {
    "calculate" - {
      "should return successful response" in new Fixture {
        wireMockServer.stubFor(
          WireMock
            .get(
              s"/calculate?accountingPeriodStart=$accountingPeriodStart&accountingPeriodEnd=$accountingPeriodEnd&profit=$profit&${exemptDistributions
                  .map(
                    "exemptDistributions" +
                      "=" + _
                  )
                  .getOrElse("")}&${associatedCompanies.map("associatedCompanies=" + _).getOrElse("")}"
            )
            .willReturn(aResponse().withBody(s"""
                                                |{
                                                |   "type": "SingleResult",
                                                |   "year": 1,
                                                |   "corporationTaxBeforeMR": 1,
                                                |   "effectiveTaxRateBeforeMR": 1,
                                                |   "corporationTax": 1,
                                                |   "effectiveTaxRate": 1,
                                                |   "marginalRelief": 1
                                                |}
                                                |""".stripMargin))
        )

        val result: MarginalReliefResult = marginalReliefCalculatorConnector
          .calculate(
            accountingPeriodStart,
            accountingPeriodEnd,
            profit,
            exemptDistributions,
            associatedCompanies
          )
          .futureValue

        result shouldEqual SingleResult(1, 1, 1, 1, 1, 1)
      }
    }

    "associatedCompaniesParameters" - {

      "should return successful response" in new Fixture {

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
            .willReturn(aResponse().withBody(s"""
                                                |{
                                                |  "type":"AskFull"
                                                |}
                                                |""".stripMargin))
        )

        val result: AssociatedCompaniesParameter = marginalReliefCalculatorConnector
          .associatedCompaniesParameters(
            accountingPeriodStart,
            accountingPeriodEnd,
            profit,
            exemptDistributions
          )
          .futureValue

        result shouldEqual AskFull
      }
    }
  }
}
