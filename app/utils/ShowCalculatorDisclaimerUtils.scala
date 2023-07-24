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

import java.time.{ LocalDate, Month }

object ShowCalculatorDisclaimerUtils {
  implicit class DateOps(date: LocalDate) {
    def isEqualOrAfter(another: LocalDate): Boolean =
      date.equals(another) || date.isAfter(another)
  }

  def showCalculatorDisclaimer(periodEnd: LocalDate): Boolean = {
    val curFinancialYear = financialYearEnd(LocalDate.now()).getYear - 1
    val calcFinancialYear = financialYearEnd(periodEnd).getYear - 1
    if (calcFinancialYear > curFinancialYear + 1) true
    else false
  }

  def financialYearEnd(date: LocalDate): LocalDate =
    (if (date.getMonth.getValue >= Month.JANUARY.getValue && date.getMonth.getValue <= Month.MARCH.getValue) {
       date
     } else {
       date.plusYears(1)
     }).withMonth(Month.MARCH.getValue).withDayOfMonth(31)
}
