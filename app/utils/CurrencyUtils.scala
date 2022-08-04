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

package utils

import java.util.{ Currency, Locale }

object CurrencyUtils {

  private val currencyFormatter = {
    val f = java.text.NumberFormat.getCurrencyInstance
    f.setCurrency(Currency.getInstance(Locale.UK))
    f
  }

  def format(value: Number): String =
    currencyFormatter.format(value).replace(".00", "")

  def roundUp(value: BigDecimal): Double =
    value.setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
}
