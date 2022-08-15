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

package connectors.sharedmodel

import julienrf.json.derived
import play.api.libs.json.{ OFormat, __ }

sealed trait FYConfig {
  def year: Int
  def mainRate: Double
}

object FYConfig {
  implicit val format: OFormat[FYConfig] =
    derived.flat.oformat[FYConfig]((__ \ "type").format[String])
}

case class FlatRateConfig(year: Int, mainRate: Double) extends FYConfig
case class MarginalReliefConfig(
  year: Int,
  lowerThreshold: Int,
  upperThreshold: Int,
  smallProfitRate: Double,
  mainRate: Double,
  marginalReliefFraction: Double
) extends FYConfig