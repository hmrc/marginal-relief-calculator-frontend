package utils

object StringUtils {
  def trimDataEntry(value: String): String =
    value.replaceAll("\n", "").replaceAll("\r", "").trim()
}
