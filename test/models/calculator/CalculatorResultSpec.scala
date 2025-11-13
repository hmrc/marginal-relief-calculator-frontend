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

package models.calculator

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

import java.time.LocalDate

class CalculatorResultSpec extends AnyFreeSpec with Matchers {

  "Calculator result serialisation" - {
    "for single result type" - {
      "should have embedded tax details" in {
        val currentDate = LocalDate.now()

        val corporationTax = 23.34
        val calculatorResult = SingleResult(FlatRate(currentDate.getYear, corporationTax, 2, 3, 4, 5, 6), 1)
        val result = Json.toJson(calculatorResult)(using CalculatorResult.writes)

        calculatorResult.totalMarginalRelief shouldBe 0.0
        calculatorResult.totalCorporationTax shouldBe corporationTax
        calculatorResult.totalCorporationTaxBeforeMR shouldBe corporationTax

        (result \ "type").as[String] shouldBe "SingleResult"
        (result \ "details" \ "type").as[String] shouldBe "FlatRate"
        (result \ "details" \ "year").as[Int] shouldBe currentDate.getYear
      }
    }

    "for dual result type" - {
      "should return tax details for both years" in {
        val year = LocalDate.now().getYear

        val calculatorResult = DualResult(
          FlatRate(year - 1, 1, 2, 3, 4, 5, 6),
          FlatRate(year, 1, 2, 3, 4, 5, 6),
          1
        )
        val result = Json.toJson(calculatorResult)(using CalculatorResult.writes)

        (result \ "type").as[String] shouldBe "DualResult"
        (result \ "year1" \ "type").as[String] shouldBe "FlatRate"
        (result \ "year1" \ "year").as[Int] shouldBe year - 1
        (result \ "year2" \ "type").as[String] shouldBe "FlatRate"
        (result \ "year2" \ "year").as[Int] shouldBe year
      }
    }
  }
}
