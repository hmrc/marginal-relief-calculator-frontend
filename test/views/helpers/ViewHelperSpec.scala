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

package views.helpers

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.twirl.api.Html

class ViewHelperSpec extends AnyFreeSpec with Matchers {

  "h1" - {
    "when style provided, should return HTML with style attribute" in {
      val viewHelper = new ViewHelper {}
      viewHelper.h1("some-text", "some-class", "some-style") shouldBe Html(
        s"""<h1 class="some-class" style="some-style">some-text</h1>"""
      )
    }

    "when style not provided, should return HTML without style attribute" in {
      val viewHelper = new ViewHelper {}
      viewHelper.h1("some-text", "some-class") shouldBe Html(s"""<h1 class="some-class" >some-text</h1>""")
    }
  }

  "h2" - {
    "when style provided, should return HTML with style attribute" in {
      val viewHelper = new ViewHelper {}
      viewHelper.h2("some-text", "some-class", "some-style") shouldBe Html(
        s"""<h2 class="some-class" style="some-style">some-text</h2>"""
      )
    }

    "when style not provided, should return HTML without style attribute" in {
      val viewHelper = new ViewHelper {}
      viewHelper.h2("some-text", "some-class") shouldBe Html(s"""<h2 class="some-class" >some-text</h2>""")
    }
  }
}
