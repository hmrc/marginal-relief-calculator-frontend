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

package forms.mappings

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.data.FormError

import java.time.LocalDate

class EndLocalDateFormatterSpec extends AnyFreeSpec with Matchers {

  private val epoch: LocalDate = LocalDate.ofEpochDay(0)
  private val epochEnd: LocalDate = epoch.plusDays(1)

  val endLocalDateFormatter = new EndLocalDateFormatter(
    "someEndDateId.invalid.key",
    "someEndDateId.allrequired.key",
    "someEndDateId.tworequired.key",
    "someEndDateId.required.key",
    "someStartDateId"
  )

  "EndLocalDateFormatter" - {
    "bind" - {
      "should bind valid data successfully" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someStartDateId.day"   -> epoch.getDayOfMonth.toString,
            "someStartDateId.month" -> epoch.getMonth.getValue.toString,
            "someStartDateId.year"  -> epoch.getYear.toString,
            "someEndDateId.day"     -> epochEnd.getDayOfMonth.toString,
            "someEndDateId.month"   -> epochEnd.getMonth.getValue.toString,
            "someEndDateId.year"    -> epochEnd.getYear.toString
          )
        )
        result shouldBe Right(epochEnd)
      }

      "should return error when end date is invalid" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someStartDateId.day"   -> epoch.getDayOfMonth.toString,
            "someStartDateId.month" -> epoch.getMonth.getValue.toString,
            "someStartDateId.year"  -> epoch.getYear.toString,
            "someEndDateId.day"     -> "invalid",
            "someEndDateId.month"   -> epochEnd.getMonth.getValue.toString,
            "someEndDateId.year"    -> epochEnd.getYear.toString
          )
        )
        result shouldBe Left(List(FormError("someEndDateId", "someEndDateId.invalid.key")))
      }

      "should error when end date is longer than a year from start date" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someStartDateId.day"   -> epoch.getDayOfMonth.toString,
            "someStartDateId.month" -> epoch.getMonth.getValue.toString,
            "someStartDateId.year"  -> epoch.getYear.toString,
            "someEndDateId.day"     -> epochEnd.getDayOfMonth.toString,
            "someEndDateId.month"   -> epochEnd.getMonth.getValue.toString,
            "someEndDateId.year"    -> epoch.plusYears(1).getYear.toString
          )
        )
        result shouldBe Left(List(FormError("someEndDateId", "accountingPeriod.error.periodIsMoreThanAYear")))
      }

      "should error when end date equal or before start date" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someStartDateId.day"   -> epochEnd.getDayOfMonth.toString,
            "someStartDateId.month" -> epochEnd.getMonth.getValue.toString,
            "someStartDateId.year"  -> epochEnd.getYear.toString,
            "someEndDateId.day"     -> epoch.getDayOfMonth.toString,
            "someEndDateId.month"   -> epoch.getMonth.getValue.toString,
            "someEndDateId.year"    -> epoch.getYear.toString
          )
        )
        result shouldBe Left(List(FormError("someEndDateId", "accountingPeriod.error.startShouldBeBeforeEnd")))
      }

      "should return end date when start date is invalid" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someStartDateId.day"   -> "invalid",
            "someStartDateId.month" -> "invalid",
            "someStartDateId.year"  -> "invalid",
            "someEndDateId.day"     -> epoch.getDayOfMonth.toString,
            "someEndDateId.month"   -> epoch.getMonth.getValue.toString,
            "someEndDateId.year"    -> epoch.getYear.toString
          )
        )
        result shouldBe Right(epoch)
      }

      "should return error when one of the date component is missing" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someEndDateId.month" -> epoch.getMonth.getValue.toString,
            "someEndDateId.year"  -> epoch.getYear.toString
          )
        )
        result shouldBe Left(List(FormError("someEndDateId", List("someEndDateId.required.key"), List("day"))))
      }

      "should return error when two of the date components are missing" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map(
            "someEndDateId.year" -> epoch.getYear.toString
          )
        )
        result shouldBe Left(
          List(FormError("someEndDateId", List("someEndDateId.tworequired.key"), List("day", "month")))
        )
      }

      "should return error when all the date components are missing" in {
        val result = endLocalDateFormatter.bind(
          "someEndDateId",
          Map.empty
        )
        result shouldBe Left(List(FormError("someEndDateId", List("someEndDateId.allrequired.key"))))
      }
    }
  }
}
