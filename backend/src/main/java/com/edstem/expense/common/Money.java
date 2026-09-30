package com.edstem.expense.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversion between API decimal amounts and the integer cents used for storage and arithmetic.
 * All balance math is done on {@code long} cents so totals are exact.
 */
public final class Money {

  public static final int SCALE = 2;

  private Money() {}

  /**
   * @throws ArithmeticException if {@code amount} has more than two decimal places or overflows
   */
  public static long toCents(BigDecimal amount) {
    return amount.setScale(SCALE, RoundingMode.UNNECESSARY).movePointRight(SCALE).longValueExact();
  }

  public static BigDecimal toAmount(long cents) {
    return BigDecimal.valueOf(cents, SCALE);
  }
}
