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

package generators

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{ Gen, Shrink }

import java.time.{ Instant, LocalDate, ZoneOffset }

trait Generators extends UserAnswersGenerator with PageGenerators with ModelGenerators with UserAnswersEntryGenerators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  def genIntersperseString(
    numbers: Gen[String],
    separator: String,
    frequencyV: Int = 1,
    frequencyN: Int = 10
  ): Gen[String] = {
    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(separator)))
    for {
      number     <- numbers
      separators <- Gen.listOfN(number.length, genValue)
    } yield number.toSeq.zip(separators).foldLeft("") {
      case (acc, (n, Some(v))) =>
        acc + n + v
      case (acc, (n, _)) =>
        acc + n
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] =
    choose[Int](min, max).map(_.toString.reverse.sliding(3, 3).mkString(",").reverse)

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def positiveIntsLargerThanMaxValue: Gen[BigInt] =
    Gen.choose[BigInt](Int.MaxValue, Long.MaxValue)

  def longsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Long.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def longsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Long.MinValue)

  def intsSmallerThanOne: Gen[BigInt] =
    Gen.choose[BigInt](Int.MinValue, 0)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.nonEmpty)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map(_.formatted("%f"))

  def positiveDecimals: Gen[BigDecimal] =
    Gen
      .choose[BigDecimal](0, Double.MaxValue)
      .suchThat(v => v.toString.matches("""^(\d*\.[1-9]\d*)$"""))

  def intsBelowValue(value: Int): Gen[BigInt] =
    Gen.choose(Int.MinValue, value)

  def intsAboveValue(value: Int): Gen[Int] =
    Gen.choose(value, Int.MaxValue)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars  <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- (minLength * 2).max(100)
    length    <- Gen.chooseNum(minLength + 1, maxLength)
    chars     <- listOfN(length, arbitrary[Char])
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  def integerBetween(min: Int, max: Int): Gen[Int] =
    choose[Int](min, max)

  def longBetween(min: Long, max: Long): Gen[Long] =
    choose[Long](min, max)
}
