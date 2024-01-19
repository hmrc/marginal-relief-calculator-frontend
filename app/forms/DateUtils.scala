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

package forms

import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, Month, ZoneId}

object DateUtils {

  private val FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy")
  private val FORMAT_FULL = DateTimeFormatter.ofPattern("d MMMM yyyy")

  implicit class DateOps(date: LocalDate) {
    def isEqualOrBefore(another: LocalDate): Boolean =
      date.equals(another) || date.isBefore(another)

    def formatDate: String = FORMAT.format(date)

    def formatDateFull: String = FORMAT_FULL.format(date)

    def govDisplayFormat(implicit messages: Messages): String = {
      val dayOfMonth = date.getDayOfMonth
      val month      = messages(s"date.${date.getMonthValue}")
      val year       = date.getYear

      s"$dayOfMonth $month $year"
    }
  }

  def financialYear(date: LocalDate): Int =
    if (date.getMonth.getValue >= Month.JANUARY.getValue && date.getMonth.getValue <= Month.MARCH.getValue) {
      date.getYear - 1
    } else {
      date.getYear
    }

  def formatInstantUTC(instant: Instant = Instant.now()): String =
    instant
      .atZone(ZoneId.of("UTC"))
      .format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss z"))
}
