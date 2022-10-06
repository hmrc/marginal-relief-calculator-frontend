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

import org.scalacheck.Arbitrary
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, PDFMetadataPage, TaxableProfitPage }

trait PageGenerators {

  implicit lazy val arbitraryPDFMetadataPage: Arbitrary[PDFMetadataPage.type] =
    Arbitrary(PDFMetadataPage)

  implicit lazy val arbitraryDistributionsIncludedPage: Arbitrary[DistributionsIncludedPage.type] =
    Arbitrary(DistributionsIncludedPage)

  implicit lazy val arbitraryDistributionPage: Arbitrary[DistributionPage.type] =
    Arbitrary(DistributionPage)

  implicit lazy val arbitraryAssociatedCompaniesPage: Arbitrary[AssociatedCompaniesPage.type] =
    Arbitrary(AssociatedCompaniesPage)

  implicit lazy val arbitraryTaxableProfitPage: Arbitrary[TaxableProfitPage.type] =
    Arbitrary(TaxableProfitPage)

  implicit lazy val arbitraryAccountingPeriodPage: Arbitrary[AccountingPeriodPage.type] =
    Arbitrary(AccountingPeriodPage)
}
