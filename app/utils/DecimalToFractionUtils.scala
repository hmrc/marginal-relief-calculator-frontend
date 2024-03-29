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

package utils

import play.api.libs.json.{ Format, Json }

import scala.annotation.tailrec

object DecimalToFractionUtils {

  case class Fraction(numerator: Int, denominator: Int)
  object Fraction {
    implicit val format: Format[Fraction] = Json.format[Fraction]
  }

  def toFraction(value: Double): Fraction = {
    val valueAsString = value.toString
    val decimals = valueAsString.substring(valueAsString.indexOf(".") + 1).length
    val denominator: Int = Math.pow(10, decimals.toDouble).toInt
    val numerator: Int = (value * denominator).toInt
    val divisor = gcd(numerator, denominator)
    Fraction(numerator / divisor, denominator / divisor)
  }

  @tailrec
  private def gcd(numerator: Int, denominator: Int): Int =
    if (denominator == 0) {
      numerator
    } else {
      gcd(denominator, numerator % denominator)
    }
}
