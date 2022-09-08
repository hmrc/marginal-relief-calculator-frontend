package utils

object StringUtils {
  def removeSpaceLineBreaks(value: String): String =
    value.replaceAll("[\n\r ]", "")
}
