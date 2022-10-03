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

package controllers

import connectors.PdfGeneratorConnector
import play.api.Environment
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.MessagesControllerComponents
import sun.misc.IOUtils
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PDFGeneratorController @Inject() (val controllerComponents: MessagesControllerComponents,
                                        environment: Environment,
                                        pdfGeneratorConnector: PdfGeneratorConnector)
                                       (implicit executionContext: ExecutionContext)extends FrontendBaseController with I18nSupport {
  def onPageLoad() = Action.async {
    val html = new String(IOUtils.readAllBytes(environment.resourceAsStream("PDFExample.html").get))
    pdfGeneratorConnector.generatePdf(html).map { case response if response.status == OK =>
      Ok(response.bodyAsBytes.toArray)
        .as("application/pdf")
        .withHeaders(
          "Content-Disposition" -> s"attachment; filename=example.pdf"
        )
    case response =>
      throw new BadRequestException("Unexpected response from pdf-generator-service : " + response.body)
    }
  }
}
