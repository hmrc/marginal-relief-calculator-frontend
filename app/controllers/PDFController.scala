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

package controllers

import akka.stream.scaladsl.StreamConverters
import controllers.actions.{ DataRequiredAction, DataRetrievalAction, IdentifierAction }
import forms.{ AccountingPeriodForm, AssociatedCompaniesForm, DistributionsIncludedForm, PDFMetadataForm, TwoAssociatedCompaniesForm }
import models.requests.DataRequest
import models.{ Distribution, PDFAddCompanyDetails, UserAnswers }
import pages._
import play.api.http.HttpEntity
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import services.{ CalculationConfigService, CalculatorService }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{ DateTime, PDFGenerator }
import views.html.{ PDFFileTemplate, PDFView }

import java.io.ByteArrayInputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class PDFController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  view: PDFView,
  pdfFileTemplate: PDFFileTemplate,
  dateTime: DateTime,
  pdfGenerator: PDFGenerator,
  calculationConfigService: CalculationConfigService,
  calculatorService: CalculatorService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  case class PDFPageRequiredParams[A](
    accountingPeriod: AccountingPeriodForm,
    taxableProfit: Int,
    distribution: Distribution,
    distributionsIncluded: Option[DistributionsIncludedForm],
    associatedCompanies: Option[AssociatedCompaniesForm],
    pdfMetadata: Option[PDFMetadataForm],
    twoAssociatedCompanies: Option[TwoAssociatedCompaniesForm],
    request: Request[A],
    userId: String,
    userAnswers: UserAnswers
  ) extends WrappedRequest[A](request)

  private val requireDomainData = new ActionRefiner[DataRequest, PDFPageRequiredParams] {
    override protected def refine[A](
      request: DataRequest[A]
    ): Future[Either[Result, PDFPageRequiredParams[A]]] =
      Future.successful {
        (
          request.userAnswers.get(AccountingPeriodPage),
          request.userAnswers.get(TaxableProfitPage),
          request.userAnswers.get(DistributionPage),
          request.userAnswers.get(DistributionsIncludedPage),
          request.userAnswers.get(AssociatedCompaniesPage),
          request.userAnswers.get(PDFMetadataPage),
          request.userAnswers.get(TwoAssociatedCompaniesPage),
          request.userAnswers.get(PDFAddCompanyDetailsPage)
        ) match {
          case (
                Some(accPeriod),
                Some(taxableProfit),
                Some(distribution),
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                maybePdfMetadata,
                maybeTwoAssociatedCompanies,
                maybeAddCompanyDetails
              ) if distribution == Distribution.No || maybeDistributionsIncluded.nonEmpty =>
            Right(
              PDFPageRequiredParams(
                accPeriod,
                taxableProfit,
                distribution,
                maybeDistributionsIncluded,
                maybeAssociatedCompanies,
                if (maybeAddCompanyDetails.exists(_.pdfAddCompanyDetails == PDFAddCompanyDetails.Yes))
                  maybePdfMetadata
                else None,
                maybeTwoAssociatedCompanies,
                request,
                request.userId,
                request.userAnswers
              )
            )
          case _ => Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    override protected def executionContext: ExecutionContext = ec
  }

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      for {
        calculatorResult <- calculatorService
                              .calculate(
                                request.accountingPeriod.accountingPeriodStartDate,
                                request.accountingPeriod.accountingPeriodEndDateOrDefault,
                                request.taxableProfit.toDouble,
                                request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).map(_.toDouble),
                                request.associatedCompanies.flatMap(_.associatedCompaniesCount),
                                None,
                                None
                              )
        config <- calculationConfigService.getAllConfigs(calculatorResult)
      } yield Ok(
        view(
          request.pdfMetadata,
          calculatorResult,
          request.accountingPeriod,
          request.taxableProfit,
          request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).getOrElse(0),
          getAssociatedCompanies,
          config,
          dateTime.currentInstant
        )
      )
    }

  def downloadPdf(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireDomainData).async { implicit request =>
      for {
        calculatorResult <- calculatorService
                              .calculate(
                                request.accountingPeriod.accountingPeriodStartDate,
                                request.accountingPeriod.accountingPeriodEndDateOrDefault,
                                request.taxableProfit.toDouble,
                                request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).map(_.toDouble),
                                request.associatedCompanies.flatMap(_.associatedCompaniesCount),
                                None,
                                None
                              )
        config <- calculationConfigService.getAllConfigs(calculatorResult)
      } yield {

        val html = pdfFileTemplate(
          pdfMetadata = request.pdfMetadata,
          calculatorResult = calculatorResult,
          accountingPeriodForm = request.accountingPeriod,
          taxableProfit = request.taxableProfit,
          distributions = request.distributionsIncluded.flatMap(_.distributionsIncludedAmount).getOrElse(0),
          associatedCompanies = getAssociatedCompanies,
          config = config,
          currentInstant = dateTime.currentInstant
        ).toString
        Ok.sendEntity(
          entity = HttpEntity.Streamed(
            data = StreamConverters.fromInputStream(() => new ByteArrayInputStream(pdfGenerator.generatePdf(html))),
            contentLength = None,
            contentType = Some("application/pdf")
          ),
          inline = false,
          fileName = Some(
            s"marginal-relief-for-corporation-tax-result-" +
              dateTime.currentInstant
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("ddMMyyyy-HHmm")) +
              ".pdf"
          )
        )
      }
    }

  private def getAssociatedCompanies(implicit request: PDFPageRequiredParams[AnyContent]) =
    request.twoAssociatedCompanies match {
      case Some(a) =>
        Right((a.associatedCompaniesFY1Count.getOrElse(0), a.associatedCompaniesFY2Count.getOrElse(0)))
      case None => Left(request.associatedCompanies.flatMap(_.associatedCompaniesCount).getOrElse(0))
    }
}
