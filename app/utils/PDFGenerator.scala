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

package utils

import com.google.inject.{ Inject, Singleton }
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.apache.pdfbox.io.IOUtils
import play.api.Environment
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, InputStream }

@Singleton
class PDFGenerator @Inject() (env: Environment) {

  private val gdsFont = IOUtils.toByteArray(env.resourceAsStream("gds.ttf").get)

  private val arialFont = IOUtils.toByteArray(env.resourceAsStream("arial.ttf").get)

  private def gdsFontStream: InputStream = new ByteArrayInputStream(gdsFont)
  private def arialFontStream: InputStream = new ByteArrayInputStream(arialFont)

  def generatePdf(html: String): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    try {
      val builder = new PdfRendererBuilder()
      builder.useFastMode()
      builder.useFont(() => arialFontStream, "Arial")
      builder.useFont(() => gdsFontStream, "GDS Transport")
      builder.usePdfUaAccessbility(true)
      builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U)
      builder.withHtmlContent(html, null)
      builder.toStream(outputStream)
      builder.run()
      outputStream.toByteArray
    } finally
      outputStream.close()
  }
}
