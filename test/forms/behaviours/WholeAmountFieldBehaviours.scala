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

package forms.behaviours

import org.scalacheck.Shrink
import play.api.data.{ Form, FormError }

trait WholeAmountFieldBehaviours extends FieldBehaviours {

  implicit val noShrinkBigInt: Shrink[BigInt] = Shrink.shrinkAny
  implicit val noShrinkBigDecimal: Shrink[BigDecimal] = Shrink.shrinkAny

  def wholeAmountField(
    form: Form[_],
    fieldName: String,
    dependentFields: Map[String, String],
    nonNumericError: FormError,
    doNotUseDecimalsError: FormError,
    outOfRangeError: FormError
  ): Unit = {

    "not bind non-numeric numbers" in {
      forAll(nonNumerics -> "nonNumeric") { nonNumeric =>
        val result = form.bind(Map(fieldName -> nonNumeric) ++ dependentFields).apply(fieldName)
        result.errors must contain only nonNumericError
      }
    }

    "not bind decimals" in {
      forAll(positiveDecimals -> "decimal") { decimal =>
        val result = form.bind(Map(fieldName -> decimal.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only doNotUseDecimalsError
      }
    }

    "not bind longs smaller than Int.MinValue" in {
      forAll(intsSmallerThanMinValue -> "tinyInt") { (num: BigInt) =>
        val result = form.bind(Map(fieldName -> num.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only outOfRangeError
      }
    }

    "not bind longs larger than Int.MaxValue" in {
      forAll(longsLargerThanMaxValue -> "massiveInt") { (num: BigInt) =>
        val result = form.bind(Map(fieldName -> num.toString) ++ dependentFields).apply(fieldName)
        result.errors must contain only outOfRangeError
      }
    }
  }
}
