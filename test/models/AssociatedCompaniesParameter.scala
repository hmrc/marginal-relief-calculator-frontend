package models

import models.associatedCompanies.{ AskBothParts, AskFull, AskOnePart, AssociatedCompaniesParameter, DontAsk, Period }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*

import java.time.LocalDate

class AssociatedCompaniesParameterSpec extends AnyFreeSpec with Matchers {

  val period1 = Period(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 6, 30))
  val period2 = Period(LocalDate.of(2023, 7, 1), LocalDate.of(2023, 12, 31))

  "AssociatedCompaniesParameter" - {

    "serialization (writes)" - {
      "should serialize AskOnePart correctly" in {
        val askOnePart = AskOnePart(period1)
        val json = AssociatedCompaniesParameter.taxDetailsFormat.writes(askOnePart)

        (json \ "type").as[String] shouldBe "AskOnePart"
        (json \ "period").as[Period] shouldBe period1
      }

      "should serialize AskBothParts correctly" in {
        val askBothParts = AskBothParts(period1, period2)
        val json = AssociatedCompaniesParameter.taxDetailsFormat.writes(askBothParts)

        (json \ "type").as[String] shouldBe "AskBothParts"
        (json \ "period1").as[Period] shouldBe period1
        (json \ "period2").as[Period] shouldBe period2
      }

      "should serialize AskFull correctly" in {
        val json = AssociatedCompaniesParameter.taxDetailsFormat.writes(AskFull)
        (json \ "type").as[String] shouldBe "AskFull"
      }

      "should serialize DontAsk correctly" in {
        val json = AssociatedCompaniesParameter.taxDetailsFormat.writes(DontAsk)
        (json \ "type").as[String] shouldBe "DontAsk"
      }
    }

    "deserialization (reads)" - {
      "should deserialize AskOnePart correctly" in {
        val json = Json.parse(
          s"""{
             |  "type": "AskOnePart",
             |  "period": ${Json.toJson(period1)}
             |}""".stripMargin
        )

        AssociatedCompaniesParameter.taxDetailsFormat.reads(json) match {
          case JsSuccess(value, _) =>
            value shouldBe AskOnePart(period1)
          case JsError(errors) =>
            fail(s"Failed to deserialize AskOnePart JSON. Errors: $errors")
        }
      }

      "should deserialize AskBothParts correctly" in {
        val json = Json.parse(
          s"""{
             |  "type": "AskBothParts",
             |  "period1": ${Json.toJson(period1)},
             |  "period2": ${Json.toJson(period2)}
             |}""".stripMargin
        )

        AssociatedCompaniesParameter.taxDetailsFormat.reads(json) match {
          case JsSuccess(value, _) =>
            value shouldBe AskBothParts(period1, period2)
          case JsError(errors) =>
            fail(s"Failed to deserialize AskBothParts JSON. Errors: $errors")
        }
      }

      "should return error for unknown type" in {
        val json = Json.parse(
          """{
            |  "type": "UnknownType"
            |}""".stripMargin
        )

        AssociatedCompaniesParameter.taxDetailsFormat.reads(json) shouldBe a[JsError]
      }

      "should return error for missing required fields" in {
        val invalidJson = Json.parse("""{ "type": "AskOnePart" }""")

        AssociatedCompaniesParameter.taxDetailsFormat.reads(invalidJson) shouldBe a[JsError]
      }
    }
  }
}
