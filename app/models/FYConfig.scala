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

package models

import play.api.libs.json._

sealed trait FYConfig {
  def year: Int
  def mainRate: Double
}

object FYConfig {
  implicit val formatRateConfig: OFormat[FlatRateConfig] = Json.format[FlatRateConfig]
  implicit val formatMarginalConfig: OFormat[MarginalReliefConfig] = Json.format[MarginalReliefConfig]

  implicit val taxDetailsFormat: OFormat[FYConfig] = new OFormat[FYConfig] {
    def reads(json: JsValue): JsResult[FYConfig] = (json \ "type").validate[String].flatMap {
      case "FlatRateConfig"       => formatRateConfig.reads(json)
      case "MarginalReliefConfig" => formatMarginalConfig.reads(json)
      case other                  => JsError(s"Unknown type: $other")
    }

    def writes(td: FYConfig): JsObject = td match {
      case fr: FlatRateConfig       => formatRateConfig.writes(fr) + ("type"     -> JsString("FlatRateConfig"))
      case mr: MarginalReliefConfig => formatMarginalConfig.writes(mr) + ("type" -> JsString("MarginalReliefConfig"))
    }
  }
}

case class FlatRateConfig(year: Int, mainRate: Double) extends FYConfig

case class MarginalReliefConfig(
  year: Int,
  lowerThreshold: Int,
  upperThreshold: Int,
  smallProfitRate: Double,
  mainRate: Double,
  marginalReliefFraction: Double
) extends FYConfig {

  def thresholdsMatch(otherConfig: MarginalReliefConfig): Boolean =
    lowerThreshold == otherConfig.lowerThreshold && upperThreshold == otherConfig.upperThreshold
}
