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

package connectors.sharedmodel

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import utils.NumberUtils.roundUp

class CalculatorResultSpec extends AnyFreeSpec with Matchers {

  private val flatRate = FlatRate(1, 11, 111, 1111, 11111, 111111, 1111111)

  private val marginalRate = MarginalRate(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)

  private val singleResult = SingleResult(marginalRate, 13)

  "SingleResult" - {
    "totalDays" - {
      "should be the days from underlying tax details" in {
        singleResult.totalDays shouldBe marginalRate.days
      }
    }

    "totalMarginalRelief" - {
      "should be the marginal relief from underlying tax details" in {
        singleResult.totalMarginalRelief shouldBe marginalRate.marginalRelief
      }
    }

    "totalCorporationTax" - {
      "should be the corporation tax from underlying tax details" in {
        singleResult.totalCorporationTax shouldBe marginalRate.corporationTax
      }
    }

    "totalCorporationTaxBeforeMR" - {
      "should be the corporation tax before MR from underlying tax details" in {
        singleResult.totalCorporationTaxBeforeMR shouldBe marginalRate.corporationTaxBeforeMR
      }
    }
  }

  private val dualResult = DualResult(flatRate, marginalRate, 13)

  "DualResult" - {
    "totalDays" - {
      "should be the total days from underlying tax details" in {
        dualResult.totalDays shouldBe flatRate.days + marginalRate.days
      }
    }

    "totalMarginalRelief" - {
      "should be the total marginal relief from underlying tax details" in {
        dualResult.totalMarginalRelief shouldBe roundUp(
          BigDecimal(marginalRate.marginalRelief)
        )
      }
    }

    "totalCorporationTax" - {
      "should be the total corporation tax from underlying tax details" in {
        dualResult.totalCorporationTax shouldBe roundUp(
          BigDecimal(flatRate.corporationTax) + BigDecimal(marginalRate.corporationTax)
        )
      }
    }

    "totalCorporationTaxBeforeMR" - {
      "should be the total corporation tax before MR from underlying tax details" in {
        dualResult.totalCorporationTaxBeforeMR shouldBe roundUp(
          BigDecimal(flatRate.corporationTax) + BigDecimal(marginalRate.corporationTaxBeforeMR)
        )
      }
    }
  }
}
