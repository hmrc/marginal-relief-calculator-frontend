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

import forms.{ AssociatedCompaniesForm, PDFMetadataForm, TwoAssociatedCompaniesForm }
import models.{ AssociatedCompanies, _ }
import org.scalacheck.Arbitrary.{ arbitrary, _ }
import org.scalacheck.{ Arbitrary, Gen }
import pages._
import play.api.libs.json.{ JsValue, Json }
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage, TwoAssociatedCompaniesPage }

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryPDFMetadataUserAnswersEntry: Arbitrary[(PDFMetadataPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[PDFMetadataPage.type]
        value <- arbitrary[PDFMetadataForm].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryTwoAssociatedCompaniesUserAnswersEntry
    : Arbitrary[(TwoAssociatedCompaniesPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[TwoAssociatedCompaniesPage.type]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDistributionsIncludedUserAnswersEntry
    : Arbitrary[(DistributionsIncludedPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[DistributionsIncludedPage.type]
        value <- arbitrary[DistributionsIncluded].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDistributionUserAnswersEntry: Arbitrary[(DistributionPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[DistributionPage.type]
        value <- arbitrary[Distribution].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAssociatedCompaniesForm: Arbitrary[AssociatedCompaniesForm] = Arbitrary {
    for {
      associatedCompany        <- arbitrary[AssociatedCompanies]
      associatedCompaniesCount <- arbitrary[Option[Int]]
    } yield AssociatedCompaniesForm(
      associatedCompany,
      associatedCompaniesCount
    )
  }

  implicit lazy val arbitraryPDFMetadataForm: Arbitrary[PDFMetadataForm] = Arbitrary {
    for {
      companyName <- Gen.option(arbitrary[String])
      utr         <- Gen.option(arbitrary[String])
    } yield PDFMetadataForm(
      companyName,
      utr
    )
  }

  implicit lazy val arbitraryTwoAssociatedCompaniesForm: Arbitrary[TwoAssociatedCompaniesForm] = Arbitrary {
    for {
      associatedCompaniesFY1Count <- Gen.option(arbitrary[Int])
      associatedCompaniesFY2Count <- Gen.option(arbitrary[Int])
    } yield TwoAssociatedCompaniesForm(
      associatedCompaniesFY1Count,
      associatedCompaniesFY2Count
    )
  }

  implicit lazy val arbitraryAssociatedCompaniesUserAnswersEntry: Arbitrary[(AssociatedCompaniesPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[AssociatedCompaniesPage.type]
        value <- arbitrary[AssociatedCompaniesForm].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryTaxableProfitUserAnswersEntry: Arbitrary[(TaxableProfitPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[TaxableProfitPage.type]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAccountingPeriodUserAnswersEntry: Arbitrary[(AccountingPeriodPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[AccountingPeriodPage.type]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }
}
