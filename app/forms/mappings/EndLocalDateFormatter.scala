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

package forms.mappings

import forms.DateUtils.DateOps
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class EndLocalDateFormatter(
  invalidKey: String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey: String,
  startDateId: String,
  args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args) {

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    lazy val missingFields = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList
      .map(x => Messages("accountingPeriod.error." + x))

    fields.count(_._2.isDefined) match {
      case 3 =>
        format(key, data)
      case 2 =>
        Left(List(FormError(key, requiredKey, missingFields ++ args)))
      case 1 =>
        Left(List(FormError(key, twoRequiredKey, missingFields ++ args)))
      case _ =>
        Left(List(FormError(key, allRequiredKey, args)))
    }
  }

  def format(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    formatDate(key, data) match {
      case Left(errors) =>
        Left(errors.map(_.copy(key = key, args = args)))
      case Right(endDate) =>
        formatDate(startDateId, data).toOption match {
          case Some(startDate) if endDate.isEqualOrBefore(startDate) =>
            Left(List(FormError(key, "accountingPeriod.error.startShouldBeBeforeEnd")))
          case Some(startDate) if endDate.isAfter(startDate.plusYears(1).minusDays(1)) =>
            Left(List(FormError(key, "accountingPeriod.error.periodIsMoreThanAYear")))
          case _ => Right(endDate)
        }
    }
}
