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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{ JsError, JsResult, JsSuccess, JsValue, Json }

class FYConfigSpec extends AnyFreeSpec with Matchers {
  "FYConfig" - {
    "derived format" - {
      "should return correct type" in {
        val flatRateConfig = FYConfig.taxDetailsFormat.writes(FlatRateConfig(2022, 0.19))
        (flatRateConfig \ "type").as[String] shouldBe "FlatRateConfig"

        val marginalReliefConfig =
          FYConfig.taxDetailsFormat.writes(MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015))
        (marginalReliefConfig \ "type").as[String] shouldBe "MarginalReliefConfig"
      }

      "should deserialize JSON to correct instance" in {
        val flatRateJson: JsValue = Json.parse(
          """{
            |  "type": "FlatRateConfig",
            |  "year": 2022,
            |  "mainRate": 0.19
            |}""".stripMargin
        )
        FYConfig.taxDetailsFormat.reads(flatRateJson) match {
          case JsSuccess(value, _) =>
            value shouldBe FlatRateConfig(2022, 0.19)
          case JsError(errors) =>
            fail(s"Failed to deserialize FlatRateConfig JSON. Errors: $errors")
        }

        val marginalReliefJson: JsValue = Json.parse(
          """{
            |  "type": "MarginalReliefConfig",
            |  "year": 2023,
            |  "lowerThreshold": 50000,
            |  "upperThreshold": 250000,
            |  "smallProfitRate": 0.19,
            |  "mainRate": 0.25,
            |  "marginalReliefFraction": 0.015
            |}""".stripMargin
        )
        FYConfig.taxDetailsFormat.reads(marginalReliefJson) match {
          case JsSuccess(value, _) =>
            value shouldBe MarginalReliefConfig(2023, 50000, 250000, 0.19, 0.25, 0.015)
          case JsError(errors) =>
            fail(s"Failed to deserialize MarginalReliefConfig JSON. Errors: $errors")
        }
      }

      "should return error for unknown type in JSON deserialization" in {
        val unknownTypeJson: JsValue = Json.parse(
          """{
            |  "type": "UnknownConfig",
            |  "year": 2022,
            |  "mainRate": 0.2
            |}""".stripMargin
        )
        FYConfig.taxDetailsFormat.reads(unknownTypeJson) match {
          case JsSuccess(_, _) =>
            fail("Deserialization should have failed for unknown type.")
          case JsError(_) =>
          // Successful test case as it should fail
        }
      }

      "should fail if required fields are missing" in {
        val invalidFlatRateJson: JsValue = Json.parse(
          """{
            |  "type": "FlatRateConfig"
            |}""".stripMargin
        )
        FYConfig.taxDetailsFormat.reads(invalidFlatRateJson) match {
          case JsSuccess(_, _) =>
            fail("Deserialization should have failed due to missing fields.")
          case JsError(_) =>
          // Successful test case as it should fail
        }

        val invalidMarginalReliefJson: JsValue = Json.parse(
          """{
            |  "type": "MarginalReliefConfig",
            |  "year": 2023
            |}""".stripMargin
        )
        FYConfig.taxDetailsFormat.reads(invalidMarginalReliefJson) match {
          case JsSuccess(_, _) =>
            fail("Deserialization should have failed due to missing fields.")
          case JsError(_) =>
          // Successful test case as it should fail
        }
      }
    }
  }
}
