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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.{ Environment, Mode }

import java.io.File

class PDFGeneratorSpec extends AnyFreeSpec with Matchers {
  "generatePdf" - {
    "should generate PDF for the given HTML" in {
      val pdfGenerator = new PDFGenerator(Environment(new File("/"), getClass.getClassLoader, Mode.Test))
      pdfGenerator
        .generatePdf("""
                       |<html>
                       |  <head>
                       |   <style>
                       |     @@page {
                       |        size: A4;
                       |        margin: 25px;
                       |        @bottom-left {
                       |            content: "Page " counter(page) "of" counter(pages);
                       |        }
                       |     }
                       |     body {
                       |       font-family:GDS Transport, Arial, sans-serif;
                       |       margin: 0px;
                       |     }
                       |     .pdf-page {
                       |       page-break-after:always;
                       |     }
                       |   </style>
                       |  </head>
                       |  <body>
                       |     <div class="pdf-page">
                       |       <p>This is page 1</p>
                       |     </div>
                       |     <div class="pdf-page">
                       |       <p>This is page 2</p>
                       |     </div>
                       |  </body>
                       |</html>
                       |""".stripMargin)
        .nonEmpty shouldBe true
    }
  }
}
