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

package forms.mappings

import models.Enumerable
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.Forms.mapping
import play.api.data.{ Form, FormError }

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
  }
}

class MappingsSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings {

  import MappingsSpec._

  "text" - {

    val testForm: Form[String] =
      Form(
        "value" -> text()
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "boolean" - {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "must bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "must bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "must not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" - {

    val testForm: Form[Int] =
      Form(
        "value" -> int()
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid integer ignoring white spaces and carriage returns" in {
      val result = testForm.bind(Map("value" -> " \n1  \r"))
      result.get mustEqual 1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "enumerable" - {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "must bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "must not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "conditional" - {

    case class TestFormData(check: String, value: Option[Int])

    val testForm = Form {
      mapping(
        "check" -> text(),
        "value" -> conditional[Int, String](
          field = int()
            .withPrefix("value"),
          conditionField = text().withPrefix("check"),
          condition = _ == "yes"
        )
      )(TestFormData.apply)(o => Some(Tuple.fromProductTyped(o)))
    }

    "must bind when condition true (value set to non-empty)" in {
      val result = testForm.bind(Map("check" -> "yes", "value" -> "1"))
      result.get mustEqual TestFormData("yes", Some(1))
    }

    "must bind when condition false (value set to empty)" in {
      val result = testForm.bind(Map("check" -> "no"))
      result.get mustEqual TestFormData("no", None)
    }

    "must return error when condition true and value empty" in {
      val result = testForm.bind(Map("check" -> "yes", "value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return error when condition true and value is invalid" in {
      val result = testForm.bind(Map("check" -> "yes", "value" -> "aaaaa"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must unbind value" in {
      val result = testForm.fill(TestFormData("yes", Some(1)))
      result.apply("value").value.value mustEqual "1"
    }

    "must unbind None value" in {
      val result = testForm.fill(TestFormData("yes", None))
      result.apply("value").value mustBe empty
    }
  }

  "wholeAmount" - {

    val testForm: Form[Int] =
      Form(
        "value" -> wholeAmount()
      )

    "must bind a valid positive whole amount" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid positive whole amount ignoring white spaces and carriage returns" in {
      val result = testForm.bind(Map("value" -> " \n1  \r"))
      result.get mustEqual 1
    }

    "must remove valid commas in input" in {
      val result = testForm.bind(Map("value" -> "1,111"))
      result.get mustEqual 1111
    }

    "must remove leading £ symbol in input" in {
      val result = testForm.bind(Map("value" -> "£1,111"))
      result.get mustEqual 1111
    }

    "must remove trailing zeroes after decimals" in {
      val result = testForm.bind(Map("value" -> "1.00"))
      result.get mustEqual 1
    }

    "must not bind when value is non-numeric" in {
      val result = testForm.bind(Map("value" -> "abc"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must not bind decimal values" in {
      val result = testForm.bind(Map("value" -> "1.11"))
      result.errors must contain(FormError("value", "error.wholeNumber"))
    }

    "must not bind values with comma in wrong places" in {
      val result = testForm.bind(Map("value" -> "111,1,11"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must not bind when value is less than Int min value" in {
      val result = testForm.bind(Map("value" -> (BigInt(Int.MinValue) - 1).toString))
      result.errors must contain(FormError("value", "error.outOfRange", List(Int.MinValue, Int.MaxValue)))
    }

    "must not bind when value is greater than Int max value" in {
      val result = testForm.bind(Map("value" -> (BigInt(Int.MaxValue) + 1).toString))
      result.errors must contain(FormError("value", "error.outOfRange", List(Int.MinValue, Int.MaxValue)))
    }

    "must not bind when value is lower than the given range" in {
      val testForm: Form[Int] =
        Form(
          "value" -> wholeAmount(valueRange = ValueRange(minValue = 1, maxValue = 100))
        )
      val result = testForm.bind(Map("value" -> "-1"))
      result.errors must contain(FormError("value", "error.lowerThanMin"))
    }

    "must not bind when value is greater than the given range" in {
      val testForm: Form[Int] =
        Form(
          "value" -> wholeAmount(valueRange = ValueRange(minValue = 1, maxValue = 100))
        )
      val result = testForm.bind(Map("value" -> "101"))
      result.errors must contain(FormError("value", "error.greaterThanMax"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(1)
      result.apply("value").value.value mustEqual "1"
    }
  }
}
