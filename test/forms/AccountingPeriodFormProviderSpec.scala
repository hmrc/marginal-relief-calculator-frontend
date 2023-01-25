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

import base.SpecBase
import forms.behaviours.DateBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import java.time.LocalDate

class AccountingPeriodFormProviderSpec extends DateBehaviours with SpecBase {

  private val application = applicationBuilder(userAnswers = None).build()
  val form = new AccountingPeriodFormProvider()(messages(application))

  "bind" - {

    "bind data to the form successfully" in {

      val startDates = datesBetween(
        min = LocalDate.of(2020, 1, 1),
        max = LocalDate.of(2020, 6, 30)
      )

      val endDates = datesBetween(
        min = LocalDate.of(2020, 7, 1),
        max = LocalDate.of(2020, 12, 31)
      )

      forAll(startDates, endDates) { (startDate, endDate) =>
        val data = buildDataMap(startDate, endDate)

        val result = form.bind(data)

        result.value.value mustEqual AccountingPeriodForm(startDate, Some(endDate))
        result.errors mustBe empty
      }
    }

    "return error when endDate is before startDate" in {

      val startDates = datesBetween(
        min = LocalDate.of(2020, 7, 1),
        max = LocalDate.of(2020, 12, 31)
      )

      val endDates = datesBetween(
        min = LocalDate.of(2020, 1, 1),
        max = LocalDate.of(2020, 6, 30)
      )

      forAll(startDates, endDates) { (startDate, endDate) =>
        val data = buildDataMap(startDate, endDate)

        val result = form.bind(data)
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("accountingPeriodEndDate", List("accountingPeriod.error.startShouldBeBeforeEnd"))
        )
      }
    }

    "return error when accounting period value contains number < 1" in {
      def data = Seq(
        s"accountingPeriodStartDate.day"   -> "1",
        s"accountingPeriodStartDate.month" -> "1",
        s"accountingPeriodStartDate.year"  -> "2023",
        s"accountingPeriodEndDate.day"     -> "30",
        s"accountingPeriodEndDate.month"   -> "12",
        s"accountingPeriodEndDate.year"    -> "2023"
      )
      (0 to 5).map { index =>
        val key = data(index)._1.split("\\.").head
        val set1 = data.updated(index, data(index)._1 -> "0").toMap
        val set2 = data.updated(index, data(index)._1 -> "-1").toMap
        val result1 = form.bind(set1)
        val result2 = form.bind(set2)
        result1.hasErrors mustBe true
        result2.hasErrors mustBe true
        result1.errors mustBe Seq(
          FormError(key, List("accountingPeriodEndDate.error.invalid"))
        )
        result2.errors mustBe Seq(
          FormError(key, List("accountingPeriodEndDate.error.invalid"))
        )
      }

    }

    "return error when accounting period is more than a year" in {
      val dates = for {
        startDate <- datesBetween(
                       min = LocalDate.of(2020, 1, 1),
                       max = LocalDate.of(2020, 12, 31)
                     )
        endMonth <- Gen.choose(13, 24)
      } yield (startDate, startDate.plusMonths(endMonth))

      forAll(dates) { case (startDate, endDate) =>
        val data = buildDataMap(startDate, endDate)

        val result = form.bind(data)
        result.hasErrors mustBe true
        result.errors mustBe Seq(
          FormError("accountingPeriodEndDate", List("accountingPeriod.error.periodIsMoreThanAYear"))
        )
      }
    }
  }

  private def buildDataMap(startDate: LocalDate, endDate: LocalDate): Map[String, String] =
    Map(
      s"accountingPeriodStartDate.day"   -> startDate.getDayOfMonth.toString,
      s"accountingPeriodStartDate.month" -> startDate.getMonthValue.toString,
      s"accountingPeriodStartDate.year"  -> startDate.getYear.toString,
      s"accountingPeriodEndDate.day"     -> endDate.getDayOfMonth.toString,
      s"accountingPeriodEndDate.month"   -> endDate.getMonthValue.toString,
      s"accountingPeriodEndDate.year"    -> endDate.getYear.toString
    )
}
