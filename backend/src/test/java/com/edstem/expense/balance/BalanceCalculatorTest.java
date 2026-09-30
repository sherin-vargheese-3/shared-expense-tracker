package com.edstem.expense.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BalanceCalculatorTest {

  private record Amount(Long memberId, Long cents) implements MemberAmount {
    @Override
    public Long getMemberId() {
      return memberId;
    }

    @Override
    public Long getCents() {
      return cents;
    }
  }

  private static MemberAmount of(long memberId, long cents) {
    return new Amount(memberId, cents);
  }

  @Test
  void combinesExpensesAndSettlements() {
    // Alice paid 90.00 split three ways; Bob then paid Alice back 30.00.
    Map<Long, Long> net =
        BalanceCalculator.netBalances(
            List.of(1L, 2L, 3L),
            List.of(of(1, 9000)),
            List.of(of(1, 3000), of(2, 3000), of(3, 3000)),
            List.of(of(2, 3000)),
            List.of(of(1, 3000)));

    assertThat(net)
        .containsExactly(Map.entry(1L, 3000L), Map.entry(2L, 0L), Map.entry(3L, -3000L));
  }

  @Test
  void membersWithNoActivityHaveZeroBalance() {
    assertThat(
            BalanceCalculator.netBalances(
                List.of(1L, 2L), List.of(), List.of(), List.of(), List.of()))
        .containsExactly(Map.entry(1L, 0L), Map.entry(2L, 0L));
  }

  @Test
  void failsLoudlyIfDataDoesNotBalance() {
    assertThatThrownBy(
            () ->
                BalanceCalculator.netBalances(
                    List.of(1L, 2L), List.of(of(1, 100)), List.of(of(2, 99)), List.of(), List.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("1 cents");
  }

  @Test
  void rejectsAmountsForUnknownMembers() {
    assertThatThrownBy(
            () ->
                BalanceCalculator.netBalances(
                    List.of(1L), List.of(of(9, 100)), List.of(), List.of(), List.of()))
        .isInstanceOf(IllegalStateException.class);
  }
}
