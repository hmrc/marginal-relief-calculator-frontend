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
import connectors.sharedmodel._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.test.{ HttpClientSupport, WireMockSupport }

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

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
        val table = Table(
          "marginalReliefResult",
          SingleResult(FlatRate(1111, 1.0, 11.0, 111.0, 0)),
          DualResult(
            MarginalRate(
              1111, 1.0, 11.0, 111.0, 1111.0, 11111.0, 0, 0, 0, 0, 0
            ),
            MarginalRate(
              2222, 2.0, 22.0, 222.0, 2222.0, 22222.0, 0, 0, 0, 0, 0
            )
          )
        )

        forAll(table) { calculatorResult: CalculatorResult =>
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
              .willReturn(aResponse().withBody(Json.toJson(calculatorResult).toString()))
          )

          val result: CalculatorResult = marginalReliefCalculatorConnector
            .calculate(
              accountingPeriodStart,
              accountingPeriodEnd,
              profit,
              exemptDistributions,
              associatedCompanies
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
  }
}
