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

package forms

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.{ Gen, Shrink }
import org.scalacheck.Gen.option
import play.api.data.{ Form, FormError }

class PDFMetadataFormProviderSpec extends StringFieldBehaviours {

  implicit val noShrinkString: Shrink[String] = Shrink.shrinkAny
  val numericStringOver: Gen[String] = for {
    i <- Gen.choose(math.pow(10, 10).toLong, math.pow(10, 11).toLong - 1)
  } yield i.toString

  val numericString: Gen[String] = for {
    i <- Gen.choose(math.pow(10, 9).toLong, math.pow(10, 10).toLong - 1)
  } yield i.toString

  val numericStringUnder: Gen[String] = for {
    i <- Gen.choose(0, Math.pow(10, 9).toLong - 1)
  } yield i.toString

  val validPDFMetadataFormGenerator: Gen[PDFMetadataForm] = for {

    companyName <- option(stringsWithMaxLength(160))
    utr         <- option(numericString)
  } yield PDFMetadataForm(companyName, utr)

  val invalidCompanyName: Gen[PDFMetadataForm] = for {
    companyName <- stringsLongerThan(160)
    utr         <- numericString
  } yield PDFMetadataForm(Some(companyName), Some(utr))

  val invalidLengthAboveUTR: Gen[PDFMetadataForm] = for {
    companyName <- stringsWithMaxLength(160)
    utr         <- numericStringOver
  } yield PDFMetadataForm(Some(companyName), Some(utr))

  val invalidLengthBelowUTR: Gen[PDFMetadataForm] = for {
    companyName <- stringsWithMaxLength(160)
    utr         <- numericStringUnder

  } yield PDFMetadataForm(Some(companyName), Some(utr))

  private val invalidCharUTR = for {
    _companyName <- stringsWithMaxLength(10)
    _utr         <- stringsWithLength10
  } yield Map(
    "companyName" -> _companyName,
    "utr"         -> _utr
  )

  val invalidCompanyNameUTR: Gen[PDFMetadataForm] = for {
    companyName <- stringsLongerThan(160)
    utr         <- numericStringOver
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
        result.errors mustBe List(FormError("companyName", Seq("pdfMetaData.companyname.error.length"), Seq(160)))
      }
    }

    "should return error when utr length is over 10" in {
      forAll(invalidLengthAboveUTR) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(FormError("utr", Seq("pdfMetaData.utr.error.length")))
      }
    }

    "should return error when utr length is under 10" in {
      forAll(invalidLengthBelowUTR) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(FormError("utr", Seq("pdfMetaData.utr.error.length")))
      }
    }

    "should return error when utr contains non numeric characters" in {
      forAll(invalidCharUTR) { invalid =>
        if (invalid.contains("companyName") && invalid.contains("utr")) {
          val result = form.bind(invalid)
          result.errors mustBe List(FormError("utr", Seq("pdfMetaData.utr.error.nonNumeric")))
        }
      }
    }

    "should return error when company name and utr length are invalid" in {
      forAll(invalidCompanyNameUTR) { invalid =>
        val result =
          form.bind(
            (invalid.companyName.map("companyName" -> _).toList ++ invalid.utr.map("utr" -> _.toString).toList).toMap
          )
        result.errors mustBe List(
          FormError("companyName", Seq("pdfMetaData.companyname.error.length"), Seq(160)),
          FormError("utr", Seq("pdfMetaData.utr.error.length"))
        )
      }
    }
  }
}
