package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import utils.CurrencyUtils.format
import utils.StringUtils.removeSpaceLineBreaks

class StringUtilsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  "removeSpaceLineBreaks" - {
    "should remove leading and/or trailing spaces and carriage returns" in {
      val table = Table[String, String](
        ("input", "expected"),
        (" test  ", "test"),
        (" te\nst", "test"),
        (" t \n es\rt ", "test")
      )
      forAll(table) { (input, expected) =>
        removeSpaceLineBreaks(input) shouldBe expected
      }
    }
  }
}
