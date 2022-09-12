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
import utils.StringUtils.removeSpaceLineBreaks

class StringUtilsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  "removeSpaceLineBreaks" - {
    "should remove leading and/or trailing spaces and carriage returns" in {
      val table = Table[String, String](
        ("input", "expected"),
        (" test  ", "test"),
        (" te\nst", "test"),
        (" t \n es\rt ", "test")
      )
      forAll(table) { (input, expected) =>
        removeSpaceLineBreaks(input) shouldBe expected
      }
    }
  }
}
