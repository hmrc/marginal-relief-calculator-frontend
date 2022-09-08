package utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import utils.StringUtils.trimDataEntry

class StringUtilsSpec extends AnyFreeSpec with Matchers with TableDrivenPropertyChecks {

  "trimDataEntry" - {
    "should remove leading and/or trailing spaces and carriage returns" in {
      val trimmedString = "test"
      val untrimmedString = Seq[String](
        " test",
        "test  ",
        "    test ",
        " \n test ",
        "test\r "
      )
      untrimmedString.forall(trimDataEntry(_) equals trimmedString)
    }
  }
}
