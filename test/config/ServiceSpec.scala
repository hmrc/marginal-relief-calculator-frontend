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

package config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ServiceSpec extends AnyFreeSpec with Matchers {
  "toString" - {
    "should return valid base url" in {
      Service("localhost", "1", "http", "/path").toString shouldBe "http://localhost:1/path"
    }

    "should return valid base url when path empty" in {
      Service("localhost", "1", "http", "").toString shouldBe "http://localhost:1"
    }
  }

  "convertToString" - {
    "should convert Service to baseUrl" in {
      val url: String = Service("localhost", "1", "http", "/path")
      url shouldBe "http://localhost:1/path"
    }
  }
}
