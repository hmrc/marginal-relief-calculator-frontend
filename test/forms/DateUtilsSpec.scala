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

package forms

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import forms.DateUtils._

import java.time.LocalDate

class DateUtilsSpec extends AnyFreeSpec with Matchers {

  private val epoch = LocalDate.ofEpochDay(0)

  "formatDate" - {
    "should format date correctly" in {
      epoch.formatDate shouldBe "1 Jan 1970"
    }
  }

  "formatDateFull" - {
    "should format date correctly" in {
      epoch.formatDateFull shouldBe "1 January 1970"
    }
  }
}