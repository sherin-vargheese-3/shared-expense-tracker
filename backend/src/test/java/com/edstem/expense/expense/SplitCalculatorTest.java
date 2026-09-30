package com.edstem.expense.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SplitCalculatorTest {

  @Test
  void splitsEvenlyWhenDivisible() {
    assertThat(SplitCalculator.equalSplit(9000, List.of(1L, 2L, 3L)))
        .containsExactly(Map.entry(1L, 3000L), Map.entry(2L, 3000L), Map.entry(3L, 3000L));
  }

  @Test
  void givesLeftoverCentsToLowestMemberIds() {
    // 100.00 / 3 = 33.34 + 33.33 + 33.33
    assertThat(SplitCalculator.equalSplit(10000, List.of(3L, 1L, 2L)))
        .containsExactly(Map.entry(1L, 3334L), Map.entry(2L, 3333L), Map.entry(3L, 3333L));
  }

  @Test
  void resultDoesNotDependOnInputOrder() {
    var a = SplitCalculator.equalSplit(1001, List.of(5L, 9L, 2L, 7L));
    var b = SplitCalculator.equalSplit(1001, List.of(7L, 2L, 9L, 5L));
    assertThat(a).isEqualTo(b).containsEntry(2L, 251L).containsEntry(9L, 250L);
  }

  @Test
  void amountSmallerThanParticipantCountLeavesSomeSharesAtZero() {
    assertThat(SplitCalculator.equalSplit(2, List.of(1L, 2L, 3L)))
        .containsExactly(Map.entry(1L, 1L), Map.entry(2L, 1L), Map.entry(3L, 0L));
  }

  @Test
  void singleParticipantTakesWholeAmount() {
    assertThat(SplitCalculator.equalSplit(4999, List.of(42L)))
        .containsExactly(Map.entry(42L, 4999L));
  }

  @Test
  void rejectsInvalidArguments() {
    assertThatThrownBy(() -> SplitCalculator.equalSplit(100, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SplitCalculator.equalSplit(100, List.of(1L, 1L)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SplitCalculator.equalSplit(-1, List.of(1L)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @RepeatedTest(200)
  void sharesAlwaysSumToAmountAndDifferByAtMostOneCent() {
    Random random = new Random();
    long amount = random.nextLong(1, 10_000_000);
    int participants = random.nextInt(1, 30);
    List<Long> ids = new ArrayList<>(LongStream.rangeClosed(1, participants).boxed().toList());
    Collections.shuffle(ids, random);

    Map<Long, Long> shares = SplitCalculator.equalSplit(amount, ids);

    assertThat(shares.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(amount);
    long min = Collections.min(shares.values());
    long max = Collections.max(shares.values());
    assertThat(max - min).isLessThanOrEqualTo(1);
  }
}
