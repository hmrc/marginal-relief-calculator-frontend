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
import utils.StringUtils._
import play.api.data.format.Formatter
import play.api.data.{ FormError, Mapping }

import scala.util.Try
import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private[mappings] def stringFormatter(errorKey: String, args: Seq[String] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None                      => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
          case Some(s)                   => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
    }

  private[mappings] def intFormatter(
    requiredKey: String,
    wholeNumberKey: String,
    nonNumericKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
        baseFormatter
          .bind(key, data)
          .map(removeSpaceLineBreaks(_).replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s =>
              nonFatalCatch
                .either(s.toInt)
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def wholeAmountFormatter(
    requiredKey: String,
    outOfRangeKey: String,
    doNotUseDecimalsKey: String,
    nonNumericKey: String,
    minKey: String,
    maxKey: String,
    minValue: Int,
    maxValue: Int,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      private val DecimalRegexp = """^-?(\d*\.\d*)$"""
      private val AmountWithCommas = """^\d{0,3}[,]?(,\d{3})*$"""
      private val TrailingZeroesAfterDecimal = """[.][0]+$"""
      private val WholeNumber = """^[-+]?\d*$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = for {
        result <- baseFormatter.bind(key, data)
        resultWithoutPoundSymbol <-
          Right(if (result.startsWith("£")) result.substring(1) else result)
        resultWithoutTrailingZeroesAfterDecimal <-
          Right(resultWithoutPoundSymbol.replaceAll(TrailingZeroesAfterDecimal, ""))
        resultWithoutCommas <- Right(resultWithoutTrailingZeroesAfterDecimal match {
                                 case s if s.matches(AmountWithCommas) => s.replace(",", "")
                                 case other                            => other
                               })
        resultWithoutSpaces = removeSpaceLineBreaks(resultWithoutCommas)
        finalResult <- resultWithoutSpaces match {
                         case s if s.matches(DecimalRegexp) =>
                           Left(Seq(FormError(key, doNotUseDecimalsKey, args)))
                         case s if !s.matches(WholeNumber) =>
                           Left(Seq(FormError(key, nonNumericKey)))
                         case s if s.matches(WholeNumber) && Try(s.toInt).isFailure =>
                           Left(Seq(FormError(key, outOfRangeKey, Seq(minValue, maxValue))))
                         case s if s.toInt < minValue =>
                           Left(Seq(FormError(key, minKey)))
                         case s if s.toInt > maxValue =>
                           Left(Seq(FormError(key, maxKey)))
                         case s => Right(s.toInt)
                       }
      } yield finalResult

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(
    implicit ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.withName(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def conditionalFormatter[A, B](
    field: Mapping[A],
    conditionField: Mapping[B],
    condition: B => Boolean
  ): Formatter[Option[A]] = new Formatter[Option[A]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[A]] =
      conditionField
        .bind(data)
        .toOption match {
        case Some(value) if condition(value) => field.bind(data).map(Some(_))
        case _                               => Right(None)
      }

    override def unbind(key: String, value: Option[A]): Map[String, String] =
      value.map(field.unbind).getOrElse(Map.empty)
  }

  private[mappings] def utrFormatter(
    requiredKey: String,
    nonNumericKey: String,
    maxKey: String,
    maxLength: Int,
    args: Seq[String] = Seq.empty
  ): Formatter[String] =
    new Formatter[String] {

      private val baseFormatter = stringFormatter(requiredKey, args)
      private val utrFormat = "^[0-9 ]*$"

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        for {
          result         <- baseFormatter.bind(key, data)
          resultNoSpaces <- Right(removeSpaceLineBreaks(result))
          finalResult <- resultNoSpaces match {
                           case s if s.length > maxLength  => Left(Seq(FormError(key, maxKey, args)))
                           case s if s.length < maxLength  => Left(Seq(FormError(key, maxKey, args)))
                           case s if !s.matches(utrFormat) => Left(Seq(FormError(key, nonNumericKey, args)))
                           //                          case s if Try(s.toString).isFailure => Left(Seq(FormError(key, nonNumericKey, args)))
                           case s => Right(s)
                         }
        } yield finalResult

      override def unbind(key: String, value: String): Map[String, String] =
        baseFormatter.unbind(key, value)
    }
}
