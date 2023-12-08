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

package forms.mappings.migrationTemp

import play.api.data.validation.Constraint
import play.api.data.{FormError, Mapping}

case class ConditionalMapping[T](condition: Condition, wrapped: Mapping[T], defaultValue: T,
                                 constraints: Seq[Constraint[T]] = Nil, keys: Set[String] = Set()) extends Mapping[T] {

  override val format: Option[(String, Seq[Any])] = wrapped.format

  val key: String = wrapped.key

  def verifying(addConstraints: Constraint[T]*): Mapping[T] =
    this.copy(constraints = constraints ++ addConstraints)

  def bind(data: Map[String, String]): Either[Seq[FormError], T] =
    if (condition(data)) wrapped.bind(data) else Right(defaultValue)

  def unbind(value: T): Map[String, String] = wrapped.unbind(value)

  def unbindAndValidate(value: T): (Map[String, String], Seq[FormError]) = wrapped.unbindAndValidate(value)

  def withPrefix(prefix: String): Mapping[T] = copy(wrapped = wrapped.withPrefix(prefix))

  val mappings: Seq[Mapping[_]] = wrapped.mappings :+ this
}

object ConditionalMapping {
  def mandatoryIfEqual[T](fieldName: String, value: String, mapping: Mapping[T]): Mapping[Option[T]] = {
    val condition: Condition = _.get(fieldName).exists(_ == value)
    ConditionalMapping(condition, MandatoryOptionalMapping(mapping, Nil), None, Seq.empty)
  }
}
