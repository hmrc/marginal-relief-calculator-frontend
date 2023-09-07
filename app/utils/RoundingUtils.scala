package utils

object RoundingUtils {
  def roundUp(value: BigDecimal): BigDecimal = value.setScale(2, BigDecimal.RoundingMode.HALF_UP)
}
