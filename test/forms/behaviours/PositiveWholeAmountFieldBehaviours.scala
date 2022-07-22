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

package forms.behaviours

import org.scalacheck.Shrink
import play.api.data.{ Form, FormError }

trait PositiveWholeAmountFieldBehaviours extends FieldBehaviours {

  implicit val noShrinkBigInt: Shrink[BigInt] = Shrink.shrinkAny
  implicit val noShrinkBigDecimal: Shrink[BigDecimal] = Shrink.shrinkAny

  def positiveWholeAmountField(
    form: Form[_],
    fieldName: String,
    dependentFields: Map[String, String],
    nonNumericError: FormError,
    doNotUseDecimalsError: FormError,
    wholeNumberError: FormError,
    outOfRangeError: FormError
  ): Unit = {

    "not bind non-numeric numbers" in {
      forAll(nonNumerics -> "nonNumeric") { nonNumeric =>
        val result = form.bind(Map(fieldName -> nonNumeric) ++ dependentFields).apply(fieldName)
        result.errors must contain only nonNumericError
      }
    }

    "not bind negative values" in {
      forAll(intsBelowValue(0) -> "negative") { negative =>
        val result = form.bind(Map(fieldName -> negative.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only outOfRangeError
      }
    }

    "not bind decimals" in {
      forAll(positiveDecimals -> "decimal") { decimal =>
        val result = form.bind(Map(fieldName -> decimal.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only doNotUseDecimalsError
      }
    }

    "not bind integers larger than Int.MaxValue" in {
      forAll(positiveIntsLargerThanMaxValue -> "massiveInt") { num: BigInt =>
        val result = form.bind(Map(fieldName -> num.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only outOfRangeError
      }
    }

    "not bind integers smaller than 1" in {
      forAll(intsSmallerThanOne) { num: BigInt =>
        val result = form.bind(Map(fieldName -> num.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only outOfRangeError
      }
    }
  }
}
