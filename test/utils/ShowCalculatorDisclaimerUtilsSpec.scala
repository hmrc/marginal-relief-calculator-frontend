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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class ShowCalculatorDisclaimerUtilsSpec extends AnyFreeSpec with Matchers {

  val curYear: LocalDate = ShowCalculatorDisclaimerUtils.financialYearEnd(LocalDate.now())

  "showCalculatorDisclaimer" - {
    "When the accounting period end date is after the 31st of March of the year after the current financial year should return true" in {
      ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(
        curYear.plusDays(1).plusYears(1)
      ) shouldBe true

      ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(
        curYear.plusYears(6)
      ) shouldBe true
    }
    "When the accounting period end date is after the 31st of March of the year before the current financial year should return false" in {
      ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(
        curYear.plusYears(1)
      ) shouldBe false

      ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(
        curYear.plusMonths(11)
      ) shouldBe false

      ShowCalculatorDisclaimerUtils.showCalculatorDisclaimer(
        curYear.minusYears(2)
      ) shouldBe false
    }
  }

  "getFinancialYearForDate" - {
    "when calendar year is the same as tax year should return expected result" in {
      ShowCalculatorDisclaimerUtils.getFinancialYearForDate(
        LocalDate.of(2023, 1, 1)
      ) shouldBe 2022
    }

    "when calendar year is not the same as tax year should return expected result" in {
      ShowCalculatorDisclaimerUtils.getFinancialYearForDate(
        LocalDate.of(2023, 5, 1)
      ) shouldBe 2023
    }

    "when date is the first day of a tax year should return expected result" in {
      ShowCalculatorDisclaimerUtils.getFinancialYearForDate(
        LocalDate.of(2023, 4, 1)
      ) shouldBe 2023
    }

    "when date is the last day of a tax year should return expected result" in {
      ShowCalculatorDisclaimerUtils.getFinancialYearForDate(
        LocalDate.of(2023, 3, 31)
      ) shouldBe 2022
    }
  }
}
