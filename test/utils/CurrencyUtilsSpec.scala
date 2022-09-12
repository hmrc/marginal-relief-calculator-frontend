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

package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import utils.CurrencyUtils.{ decimalFormat, format }
import utils.NumberUtils.roundUp

class CurrencyUtilsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {
  "roundUp" - {
    "should round up values" in {
      val table = Table(
        ("input", "expected"),
        (BigDecimal(0), 0),
        (BigDecimal(1), 1),
        (BigDecimal(1.1), 1.1),
        (BigDecimal(1.11), 1.11),
        (BigDecimal(1.15), 1.15),
        (BigDecimal(1.16), 1.16),
        (BigDecimal(1.111), 1.11),
        (BigDecimal(1.115), 1.12),
        (BigDecimal(1.116), 1.12)
      )
      forAll(table) { (input, expected) =>
        roundUp(input) shouldBe expected
      }
    }
  }

  "format" - {
    "should format the given number in GBP currency" in {
      val table = Table[Double, String](
        ("input", "expected"),
        (0, "£0"),
        (0.01, "£0.01"),
        (0.1, "£0.10"),
        (1, "£1"),
        (100, "£100"),
        (1000, "£1,000"),
        (1000.01, "£1,000.01"),
        (1000.1, "£1,000.10"),
        (10000, "£10,000"),
        (100000, "£100,000"),
        (1000000, "£1,000,000")
      )
      forAll(table) { (input, expected) =>
        format(input) shouldBe expected
      }
    }
  }
  "decimalFormat" - {
    "should format the given number in GBP currency" in {
      val table = Table[Double, String](
        ("input", "expected"),
        (0, "£0.00"),
        (0.01, "£0.01"),
        (0.1, "£0.10"),
        (1, "£1.00"),
        (100, "£100.00"),
        (1000, "£1,000.00"),
        (1000.01, "£1,000.01"),
        (1000.1, "£1,000.10"),
        (10000, "£10,000.00"),
        (100000, "£100,000.00"),
        (1000000, "£1,000,000.00")
      )
      forAll(table) { (input, expected) =>
        decimalFormat(input) shouldBe expected
      }
    }
  }
}
