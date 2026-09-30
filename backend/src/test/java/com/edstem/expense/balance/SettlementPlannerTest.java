package com.edstem.expense.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SettlementPlannerTest {

  private static Map<Long, Long> balances(long... cents) {
    Map<Long, Long> map = new LinkedHashMap<>();
    for (int i = 0; i < cents.length; i++) {
      map.put((long) i + 1, cents[i]);
    }
    return map;
  }

  /** Applies the transfers and asserts every balance ends at exactly zero. */
  private static void assertSettlesEverything(Map<Long, Long> balances, List<Transfer> plan) {
    Map<Long, Long> remaining = new HashMap<>(balances);
    for (Transfer t : plan) {
      assertThat(t.amountCents()).isPositive();
      remaining.merge(t.fromMemberId(), t.amountCents(), Long::sum);
      remaining.merge(t.toMemberId(), -t.amountCents(), Long::sum);
    }
    assertThat(remaining.values()).allMatch(v -> v == 0);
  }

  /**
   * Independent exhaustive search for the minimum number of transfers (classic backtracking), used
   * as an oracle to check the planner's optimality.
   */
  private static int bruteForceMinimum(long[] debt, int start) {
    while (start < debt.length && debt[start] == 0) {
      start++;
    }
    if (start == debt.length) {
      return 0;
    }
    int best = Integer.MAX_VALUE;
    for (int i = start + 1; i < debt.length; i++) {
      if (Long.signum(debt[i]) == -Long.signum(debt[start])) {
        debt[i] += debt[start];
        best = Math.min(best, 1 + bruteForceMinimum(debt, start + 1));
        debt[i] -= debt[start];
      }
    }
    return best;
  }

  private static long[] randomZeroSumBalances(Random random, int size, long maxAbs) {
    long[] cents = new long[size];
    long sum = 0;
    for (int i = 0; i < size - 1; i++) {
      cents[i] = random.nextLong(-maxAbs, maxAbs + 1);
      sum += cents[i];
    }
    cents[size - 1] = -sum;
    return cents;
  }

  @Test
  void noPaymentsWhenEveryoneIsSettled() {
    assertThat(SettlementPlanner.plan(Map.of())).isEmpty();
    assertThat(SettlementPlanner.plan(balances(0, 0, 0))).isEmpty();
  }

  @Test
  void singleDebtorPaysSingleCreditor() {
    assertThat(SettlementPlanner.plan(balances(2500, -2500)))
        .containsExactly(new Transfer(2, 1, 2500));
  }

  @Test
  void oneCreditorIsPaidByEveryDebtor() {
    // Alice paid 90.00 split three ways.
    var plan = SettlementPlanner.plan(balances(6000, -3000, -3000));
    assertThat(plan).containsExactly(new Transfer(2, 1, 3000), new Transfer(3, 1, 3000));
  }

  @Test
  void findsTheMinimumWhereGreedyMatchingDoesNot() {
    // Greedy largest-debtor/largest-creditor matching needs 5 payments here; the optimum is 4
    // because {-3, +3} can be settled separately from {-8, -2, +4, +6}.
    Map<Long, Long> balances = balances(-800, -300, -200, 300, 400, 600);

    List<Transfer> plan = SettlementPlanner.plan(balances);

    assertThat(plan).hasSize(4).contains(new Transfer(2, 4, 300));
    assertSettlesEverything(balances, plan);
  }

  @Test
  void resultIsDeterministic() {
    Map<Long, Long> balances = balances(-500, -500, 250, 250, 500);
    assertThat(SettlementPlanner.plan(balances)).isEqualTo(SettlementPlanner.plan(balances));
  }

  @Test
  void rejectsBalancesThatDoNotSumToZero() {
    assertThatThrownBy(() -> SettlementPlanner.plan(balances(100, -99)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @RepeatedTest(300)
  void matchesBruteForceMinimumOnRandomSmallGroups() {
    Random random = new Random();
    // Small magnitudes make zero-sum subgroups common, which is where optimality matters.
    long[] cents = randomZeroSumBalances(random, random.nextInt(2, 9), 6);
    Map<Long, Long> balances = balances(cents);

    List<Transfer> plan = SettlementPlanner.plan(balances);

    assertSettlesEverything(balances, plan);
    assertThat(plan).hasSize(bruteForceMinimum(cents.clone(), 0));
  }

  @Test
  void exactSolverHandlesTheMaximumSizeQuickly() {
    Random random = new Random(42);
    long[] cents = randomZeroSumBalances(random, SettlementPlanner.MAX_EXACT_BALANCES, 50);
    Map<Long, Long> balances = balances(cents);

    long start = System.nanoTime();
    List<Transfer> plan = SettlementPlanner.plan(balances);
    long millis = (System.nanoTime() - start) / 1_000_000;

    assertSettlesEverything(balances, plan);
    assertThat(millis).isLessThan(2_000);
  }

  @RepeatedTest(20)
  void largeGroupsFallBackToGreedyWithAtMostNMinusOnePayments() {
    Random random = new Random();
    long[] cents = randomZeroSumBalances(random, 40, 100_000);
    Map<Long, Long> balances = balances(cents);
    long nonZero = balances.values().stream().filter(v -> v != 0).count();

    List<Transfer> plan = SettlementPlanner.plan(balances);

    assertSettlesEverything(balances, plan);
    assertThat(plan.size()).isLessThanOrEqualTo((int) Math.max(0, nonZero - 1));
  }
}
