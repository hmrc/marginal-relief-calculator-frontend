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

package models.associatedCompanies

import play.api.libs.json.{ JsError, JsObject, JsResult, JsString, JsValue, Json, OFormat }

sealed trait AssociatedCompaniesParameter

object AssociatedCompaniesParameter {
  implicit val formatAskOnePart: OFormat[AskOnePart] = Json.format[AskOnePart]
  implicit val formatAskAskBothParts: OFormat[AskBothParts] = Json.format[AskBothParts]

  implicit val taxDetailsFormat: OFormat[AssociatedCompaniesParameter] = new OFormat[AssociatedCompaniesParameter] {
    def reads(json: JsValue): JsResult[AssociatedCompaniesParameter] = (json \ "type").validate[String].flatMap {
      case "AskOnePart"   => formatAskOnePart.reads(json)
      case "AskBothParts" => formatAskAskBothParts.reads(json)
      case other          => JsError(s"Unknown type: $other")
    }

    def writes(td: AssociatedCompaniesParameter): JsObject = td match {
      case fr: AskOnePart   => formatAskOnePart.writes(fr) + ("type"      -> JsString("AskOnePart"))
      case mr: AskBothParts => formatAskAskBothParts.writes(mr) + ("type" -> JsString("AskBothParts"))
      case AskFull          => Json.obj("type" -> "AskFull")
      case DontAsk          => Json.obj("type" -> "DontAsk")
    }

  }
}

case object DontAsk extends AssociatedCompaniesParameter

case object AskFull extends AssociatedCompaniesParameter
case class AskOnePart(period: Period) extends AssociatedCompaniesParameter
case class AskBothParts(period1: Period, period2: Period) extends AssociatedCompaniesParameter
