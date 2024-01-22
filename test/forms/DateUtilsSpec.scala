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

import forms.DateUtils.{ DateOps, financialYear }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers

import java.time.LocalDate

class DateUtilsSpec extends AnyFreeSpec with Matchers {

  private val epoch = LocalDate.ofEpochDay(0)
  private implicit val messages: Messages = Helpers.stubMessages(messagesApi =
    Helpers.stubMessagesApi(
      messages = Map(
        "en" -> Map(
          "date.1"  -> "January",
          "date.2"  -> "February",
          "date.3"  -> "March",
          "date.4"  -> "April",
          "date.5"  -> "May",
          "date.6"  -> "June",
          "date.7"  -> "July",
          "date.8"  -> "August",
          "date.9"  -> "September",
          "date.10" -> "October",
          "date.11" -> "November",
          "date.12" -> "December"
        )
      )
    )
  )

  "govDisplayFormat" - {
    "should format date correctly using messages for month names" in {
      epoch.govDisplayFormat shouldBe "1 January 1970"
    }
  }

  "financialYear" - {

    "should return previous year when month is between January and March" in {
      financialYear(epoch) shouldBe epoch.getYear - 1
    }

    "should return current year when month is between April and December" in {
      financialYear(epoch.plusMonths(3)) shouldBe epoch.getYear
    }

  }
}
