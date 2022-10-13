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

import models.{ CheckMode, NormalMode }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import views.helpers.BackLinkHelper.backLinkOrDefault

class BackLinkHelperSpec extends AnyFreeSpec with Matchers {
  "backLinkOrDefault" - {
    "should return check your answers path in CheckMode" in {
      implicit val request: RequestHeader = FakeRequest(GET, "/")
      backLinkOrDefault("/", CheckMode) shouldBe controllers.routes.CheckYourAnswersController.onPageLoad
        .path()
    }

    "should return default path when session does not contain visited links" in {
      implicit val request: RequestHeader = FakeRequest(GET, "/")
      backLinkOrDefault("/", NormalMode) shouldBe "/?back=true"
    }

    "should return head value of the tail from the visited links list, when it has 2 or more values" in {
      implicit val request: RequestHeader = FakeRequest(GET, "/")
        .withSession("visitedLinks" -> Json.toJson(List("/path-2", "/path-1")).toString)

      backLinkOrDefault("/", NormalMode) shouldBe "/path-1?back=true"
    }

    "should return default path, when visited links list has 1 value" in {
      implicit val request: RequestHeader = FakeRequest(GET, "/")
        .withSession("visitedLinks" -> Json.toJson(List("/path-1")).toString)

      backLinkOrDefault("/", NormalMode) shouldBe "/?back=true"
    }
  }
}
