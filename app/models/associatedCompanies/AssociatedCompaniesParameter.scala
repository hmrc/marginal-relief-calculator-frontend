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

import julienrf.json.derived
import play.api.libs.json.{ OFormat, __ }

sealed trait AssociatedCompaniesParameter

object AssociatedCompaniesParameter {
  implicit val format: OFormat[AssociatedCompaniesParameter] =
    derived.flat.oformat[AssociatedCompaniesParameter]((__ \ "type").format[String])
}

case object DontAsk extends AssociatedCompaniesParameter

case object AskFull extends AssociatedCompaniesParameter
case class AskOnePart(period: Period) extends AssociatedCompaniesParameter
case class AskBothParts(period1: Period, period2: Period) extends AssociatedCompaniesParameter
