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

import play.api.data.{ FieldMapping, Mapping }
import play.api.data.Forms.of
import models.Enumerable

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", args: Seq[String] = Seq.empty): FieldMapping[String] =
    of(using stringFormatter(errorKey, args))

  protected def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    of(using intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def wholeAmount(
    requiredKey: String = "error.required",
    outOfRangeKey: String = "error.outOfRange",
    doNotUseDecimalsKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    keyRange: KeyRange = KeyRange("error.lowerThanMin", "error.greaterThanMax"),
    valueRange: ValueRange = ValueRange(Integer.MIN_VALUE, Integer.MAX_VALUE),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    of(using 
      wholeAmountFormatter(
        requiredKey,
        outOfRangeKey,
        doNotUseDecimalsKey,
        nonNumericKey,
        keyRange,
        valueRange,
        args
      )
    )

  protected def utrMapper(
    requiredKey: String = "error.required",
    nonNumericKey: String = "pdfMetaData.utr.error.nonNumeric",
    maxKey: String = "pdfMetaData.utr.error.length",
    maxLength: Int = 10,
    args: Seq[String] = Seq.empty
  ): FieldMapping[String] =
    of(using 
      utrFormatter(
        requiredKey,
        nonNumericKey,
        maxKey,
        maxLength,
        args
      )
    )

  protected def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(using booleanFormatter(requiredKey, invalidKey, args))

  protected def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid",
    args: Seq[String] = Seq.empty
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(using enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def conditional[A, B](
    field: Mapping[A],
    conditionField: Mapping[B],
    condition: B => Boolean
  ): FieldMapping[Option[A]] = of(using conditionalFormatter(field, conditionField, condition))
}
