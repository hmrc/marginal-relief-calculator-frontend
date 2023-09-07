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

package models.calculator

import models.{ FYConfig, FlatRateConfig, MarginalReliefConfig }
import utils.DateUtils

import java.time.LocalDate

sealed trait FyDataWrapper {
  val associatedCompaniesAp: Option[Int]
  val daysInAp: Int
}

case class SingleFyDataWrapper[C <: FYConfig](fy1Values: FyValues[C], associatedCompaniesAp: Option[Int], daysInAp: Int)
    extends FyDataWrapper

case class DualFyDataWrapper[C <: FYConfig](
  fy1Values: FyValues[C],
  fy2Values: FyValues[C],
  associatedCompaniesAp: Option[Int],
  daysInAp: Int
) extends FyDataWrapper

case class MixedFyDataWrapper(
  fyFlatValues: FyValues[FlatRateConfig],
  fyMarginalValues: FyValues[MarginalReliefConfig],
  associatedCompaniesAp: Option[Int],
  daysInAp: Int
) extends FyDataWrapper

object FyDataWrapper extends DateUtils {

  def constructSingleYear[C <: FYConfig](
    fy1: Int,
    accountingPeriodStart: LocalDate,
    daysInAP: Int,
    fyEndForAPStartDate: LocalDate,
    profit: BigDecimal,
    distributions: BigDecimal,
    associatedCompaniesFy1: Option[Int],
    associatedCompaniesAp: Option[Int],
    fY1Config: C
  ): SingleFyDataWrapper[C] = {

    val apDaysInFY = daysBetweenInclusive(accountingPeriodStart, fyEndForAPStartDate)
    val fy1Values: FyValues[C] = new FyValues[C](
      fy = fy1,
      fYConfig = fY1Config,
      apDaysInFY = daysInAP,
      apFYRatio = BigDecimal(apDaysInFY) / daysInAP,
      adjustedProfit = profit,
      adjustedDistributions = distributions,
      adjustedAugmentedProfit = profit + distributions,
      associatedCompaniesFY = associatedCompaniesFy1
    )

    SingleFyDataWrapper[C](
      fy1Values = fy1Values,
      associatedCompaniesAp = associatedCompaniesAp,
      daysInAp = daysInAP
    )
  }

  def constructDualYear[C <: FYConfig](
    fy1: Int,
    fy2: Int,
    accountingPeriodStart: LocalDate,
    accountingPeriodEnd: LocalDate,
    daysInAP: Int,
    fyEndForAPStartDate: LocalDate,
    profit: BigDecimal,
    distributions: BigDecimal,
    associatedCompaniesFy1: Option[Int],
    associatedCompaniesFy2: Option[Int],
    associatedCompaniesAp: Option[Int],
    fY1Config: C,
    fY2Config: C
  ): DualFyDataWrapper[C] = {

    val fy1Values: FyValues[C] = FyValues[C](
      fy = fy1,
      fyOrApStart = accountingPeriodStart,
      fyOrApEnd = fyEndForAPStartDate,
      daysInAp = daysInAP,
      profit = profit,
      distributions = distributions,
      associatedCompaniesFy = associatedCompaniesFy1,
      fyConfig = fY1Config
    )

    val fy2Values: FyValues[C] = FyValues[C](
      fy = fy2,
      fyOrApStart = fyEndForAPStartDate.plusDays(1),
      fyOrApEnd = accountingPeriodEnd,
      daysInAp = daysInAP,
      profit = profit,
      distributions = distributions,
      associatedCompaniesFy = associatedCompaniesFy2,
      fyConfig = fY2Config
    )

    DualFyDataWrapper[C](
      fy1Values = fy1Values.copyWithConcreteConfigType(fY1Config),
      fy2Values = fy2Values.copyWithConcreteConfigType(fY2Config),
      associatedCompaniesAp = associatedCompaniesAp,
      daysInAp = daysInAP
    )
  }

  def constructMixedYear(
    fyFlat: Int,
    fyMarginal: Int,
    flatStart: LocalDate,
    flatEnd: LocalDate,
    marginalStart: LocalDate,
    marginalEnd: LocalDate,
    daysInAP: Int,
    profit: BigDecimal,
    distributions: BigDecimal,
    associatedCompaniesFlat: Option[Int],
    associatedCompaniesMarginal: Option[Int],
    associatedCompaniesAp: Option[Int],
    fYFlatConfig: FlatRateConfig,
    fYMarginalConfig: MarginalReliefConfig
  ): MixedFyDataWrapper = {

    val fyFlatValues: FyValues[FlatRateConfig] = FyValues[FlatRateConfig](
      fy = fyFlat,
      fyOrApStart = flatStart,
      fyOrApEnd = flatEnd,
      daysInAp = daysInAP,
      profit = profit,
      distributions = distributions,
      associatedCompaniesFy = associatedCompaniesFlat,
      fyConfig = fYFlatConfig
    )

    val fyMarginalValues: FyValues[MarginalReliefConfig] = FyValues[MarginalReliefConfig](
      fy = fyMarginal,
      fyOrApStart = marginalStart,
      fyOrApEnd = marginalEnd,
      daysInAp = daysInAP,
      profit = profit,
      distributions = distributions,
      associatedCompaniesFy = associatedCompaniesMarginal,
      fyConfig = fYMarginalConfig
    )

    MixedFyDataWrapper(
      fyFlatValues = fyFlatValues,
      fyMarginalValues = fyMarginalValues,
      associatedCompaniesAp = associatedCompaniesAp,
      daysInAp = daysInAP
    )
  }
}
