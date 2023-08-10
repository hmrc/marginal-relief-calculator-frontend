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

package config

import com.google.inject.{ Inject, Singleton }
import connectors.sharedmodel.CalculatorConfig
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Configuration
import play.api.i18n.Lang

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  val host: String = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  // private val contactHost = configuration.get[String]("contact-frontend.host")
  // private val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")

  // def feedbackUrl(implicit request: RequestHeader): String = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String = s"$exitSurveyBaseUrl/feedback/marginal-relief-calculator-frontend"

  val marginalReliefCalculatorUrl: String =
    configuration.get[Service]("microservice.services.marginal-relief-calculator-backend").baseUrl

  val languageTranslationEnabled: Boolean = configuration.get[Boolean]("features.welsh-translation")
  val reworkEnabled: Boolean = configuration.getOptional[Boolean](path = "features.rework-enabled").getOrElse(false)

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val authEnabled: Boolean = configuration.get[Boolean]("auth.enabled")
  val basicAuthRealm: Option[String] = configuration.getOptional[String]("auth.basic.realm")
  val basicAuthUser: Option[String] = configuration.getOptional[String]("auth.basic.username")
  val basicAuthPassword: Option[String] = configuration.getOptional[String]("auth.basic.password")

  val calculatorConfig: CalculatorConfig =
    CalculatorConfigParser
      .parse(configuration)
      .fold(
        invalidConfigError => {
          val errors = invalidConfigError.map(_.message).toList.mkString(", ")
          logger.error(s"Failed to parse calculator-config. Errors are '$errors'")
          throw new RuntimeException("Failed to parse calculator-config")
        },
        fyConfigs => CalculatorConfig(fyConfigs)
      )
}
