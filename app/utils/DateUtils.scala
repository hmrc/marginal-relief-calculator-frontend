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

package utils

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneId}

object DateUtils extends App {

  def daysBetweenInclusive(start: LocalDate, end: LocalDate): Int =
    (start.until(end, ChronoUnit.DAYS) + 1).toInt

  def formatInstantUTC(instant: Instant = Instant.now()): String =
    instant
      .atZone(ZoneId.of("UTC"))
      .format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss z"))

  def daysInFY(year: Int): Int = {
    val start = LocalDate.of(year, 4, 1)
    daysBetweenInclusive(start, start.plusYears(1).withMonth(3).withDayOfMonth(31))
  }
}
