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

package controllers.actions

import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm }
import models.{ Distribution, UserAnswers }
import models.requests.DataRequest
import pages.{ AccountingPeriodPage, AssociatedCompaniesPage, DistributionPage, DistributionsIncludedPage, TaxableProfitPage }
import play.api.mvc.Results.Redirect
import play.api.mvc.{ ActionRefiner, Request, Result, WrappedRequest }

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

case class PDFMetadataPageRequiredParams[A](
  accountingPeriod: AccountingPeriodForm,
  taxableProfit: Int,
  distribution: Distribution,
  distributionsIncluded: Option[DistributionsIncludedForm],
  associatedCompanies: Option[AssociatedCompaniesForm],
  request: Request[A],
  userId: String,
  userAnswers: UserAnswers
) extends WrappedRequest[A](request)

class PDFRequiredDataAction @Inject() (implicit val executionContext: ExecutionContext)
    extends ActionRefiner[DataRequest, PDFMetadataPageRequiredParams] {
  override protected def refine[A](
    request: DataRequest[A]
  ): Future[Either[Result, PDFMetadataPageRequiredParams[A]]] =
    Future.successful {
      (
        request.userAnswers.get(AccountingPeriodPage),
        request.userAnswers.get(TaxableProfitPage),
        request.userAnswers.get(DistributionPage),
        request.userAnswers.get(DistributionsIncludedPage),
        request.userAnswers.get(AssociatedCompaniesPage)
      ) match {
        case (
              Some(accPeriod),
              Some(taxableProfit),
              Some(distribution),
              maybeDistributionsIncluded,
              maybeAssociatedCompanies
            ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
          Right(
            PDFMetadataPageRequiredParams(
              accPeriod,
              taxableProfit,
              distribution,
              maybeDistributionsIncluded,
              maybeAssociatedCompanies,
              request,
              request.userId,
              request.userAnswers
            )
          )
        case _ => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
