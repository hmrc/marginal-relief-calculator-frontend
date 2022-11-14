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

package forms

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.{ Gen, Shrink }
import org.scalacheck.Gen.option
import play.api.data.{ Form, FormError }

class PDFMetadataFormProviderSpec extends StringFieldBehaviours {

  implicit val noShrinkString: Shrink[String] = Shrink.shrinkAny
  val longUTR = 123456789012345L
  val over15LongUTR = 123456789012345678L
  val validPDFMetadataFormGenerator: Gen[PDFMetadataForm] = for {

    companyName <- option(stringsWithMaxLength(160))
    utr         <- option(longBetween(0, longUTR))
  } yield PDFMetadataForm(companyName, utr)

  val invalidCompanyName: Gen[PDFMetadataForm] = for {
    companyName <- stringsLongerThan(160)
    utr         <- longBetween(0, longUTR)
  } yield PDFMetadataForm(Some(companyName), Some(utr))

  val invalidUTR: Gen[PDFMetadataForm] = for {
    companyName <- stringsWithMaxLength(160)
    utr         <- longBetween(longUTR, over15LongUTR)
  } yield PDFMetadataForm(Some(companyName), Some(utr))

  val invalidCompanyNameUTR: Gen[PDFMetadataForm] = for {
    companyName <- stringsLongerThan(160)
    utr         <- longBetween(0, longUTR)
  } yield PDFMetadataForm(Some(companyName), Some(utr))

  private val form: Form[PDFMetadataForm] = new PDFMetadataFormProvider()()

  "bind" - {

    "should bind empty map" in {
      form.bind(Map.empty[String, String]).value mustBe Some(PDFMetadataForm(None, None))
    }

    "should bind empty strings" in {
      form.bind(Map("companyName" -> "", "utr" -> "")).value mustBe Some(PDFMetadataForm(None, None))
    }

    "should bind valid data" in {
      forAll(validPDFMetadataFormGenerator) { valid =>
        form
          .bind((valid.companyName.map("companyName" -> _).toList ++ valid.utr.map("utr" -> _.toString).toList).toMap)
          .value mustBe Some(valid)
      }
    }

    "should return error when company name is invalid" in {
      forAll(invalidCompanyName) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(FormError("companyName", Seq("pDFMetadata.companyname.error.length"), Seq(160)))
      }
    }

    "should return error when utr is invalid" in {
      forAll(invalidUTR) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(FormError("utr", Seq("pDFMetadata.utr.error.length"), Seq()))
      }
    }

    "should return error when company name and utr are invalid" in {
      forAll(invalidCompanyNameUTR) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(
          FormError("companyName", Seq("pDFMetadata.companyname.error.length"), Seq(160)),
          FormError("utr", Seq("pDFMetadata.utr.error.length"), Seq())
        )
      }
    }
  }
}
