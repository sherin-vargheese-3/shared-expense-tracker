package com.edstem.expense.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

  @ParameterizedTest
  @CsvSource({"100, 10000", "100.5, 10050", "0.01, 1", "12.34, 1234", "1E+2, 10000"})
  void convertsAmountsToCents(String amount, long cents) {
    assertThat(Money.toCents(new BigDecimal(amount))).isEqualTo(cents);
  }

  @Test
  void rejectsMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> Money.toCents(new BigDecimal("1.005")))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void convertsCentsToTwoDecimalAmount() {
    assertThat(Money.toAmount(1234)).isEqualTo(new BigDecimal("12.34"));
    assertThat(Money.toAmount(5).toPlainString()).isEqualTo("0.05");
    assertThat(Money.toAmount(-250).toPlainString()).isEqualTo("-2.50");
  }
}
