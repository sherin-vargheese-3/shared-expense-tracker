package com.edstem.expense.balance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Computes the fewest payments that bring every balance to zero.
 *
 * <p>With {@code n} non-zero balances, the optimum is {@code n - k}, where {@code k} is the largest
 * number of disjoint groups the balances can be split into such that each group sums to zero
 * (each group of size {@code s} then settles in {@code s - 1} payments). Finding {@code k} is
 * NP-hard in general, so it is solved exactly with a bitmask DP over subsets in {@code O(2^n * n)}
 * for up to {@value #MAX_EXACT_BALANCES} non-zero balances. Beyond that the planner falls back to
 * greedy matching, which needs at most {@code n - 1} payments but may not be minimal.
 */
public final class SettlementPlanner {

  static final int MAX_EXACT_BALANCES = 20;

  private SettlementPlanner() {}

  /**
   * @param balances net balance per member in cents; positive means the member is owed money.
   *     Must sum to zero.
   * @return payments ordered by amount (largest first), then by member ids
   */
  public static List<Transfer> plan(Map<Long, Long> balances) {
    long total = balances.values().stream().mapToLong(Long::longValue).sum();
    if (total != 0) {
      throw new IllegalArgumentException("balances must sum to zero but sum to " + total);
    }

    List<Balance> nonZero =
        balances.entrySet().stream()
            .filter(e -> e.getValue() != 0)
            .map(e -> new Balance(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(Balance::memberId))
            .toList();

    List<Transfer> transfers = new ArrayList<>();
    if (nonZero.size() <= MAX_EXACT_BALANCES) {
      for (List<Balance> group : zeroSumGroups(nonZero)) {
        transfers.addAll(greedy(group));
      }
    } else {
      transfers.addAll(greedy(nonZero));
    }

    transfers.sort(
        Comparator.comparingLong(Transfer::amountCents)
            .reversed()
            .thenComparingLong(Transfer::fromMemberId)
            .thenComparingLong(Transfer::toMemberId));
    return transfers;
  }

  /**
   * Splits balances into the maximum number of disjoint zero-sum groups.
   *
   * <p>{@code best[mask]} is the largest number of zero-sum prefixes achievable by some ordering of
   * the members in {@code mask}. Consecutive zero-sum prefixes delimit zero-sum groups, so {@code
   * best[full]} is the maximum group count. The ordering is recovered by walking back from the full
   * mask.
   */
  private static List<List<Balance>> zeroSumGroups(List<Balance> balances) {
    int n = balances.size();
    if (n == 0) {
      return List.of();
    }
    int full = (1 << n) - 1;
    long[] sum = new long[full + 1];
    int[] best = new int[full + 1];

    for (int mask = 1; mask <= full; mask++) {
      int lowest = Integer.numberOfTrailingZeros(mask);
      sum[mask] = sum[mask & (mask - 1)] + balances.get(lowest).cents();
      int max = 0;
      for (int rest = mask; rest != 0; rest &= rest - 1) {
        max = Math.max(max, best[mask ^ Integer.lowestOneBit(rest)]);
      }
      best[mask] = max + (sum[mask] == 0 ? 1 : 0);
    }

    // Recover an optimal ordering by peeling members off the end.
    int[] order = new int[n];
    int mask = full;
    for (int pos = n - 1; pos >= 0; pos--) {
      int target = best[mask] - (sum[mask] == 0 ? 1 : 0);
      for (int rest = mask; rest != 0; rest &= rest - 1) {
        int bit = Integer.lowestOneBit(rest);
        if (best[mask ^ bit] == target) {
          order[pos] = Integer.numberOfTrailingZeros(bit);
          mask ^= bit;
          break;
        }
      }
    }

    List<List<Balance>> groups = new ArrayList<>();
    List<Balance> current = new ArrayList<>();
    long running = 0;
    for (int index : order) {
      Balance balance = balances.get(index);
      current.add(balance);
      running += balance.cents();
      if (running == 0) {
        groups.add(current);
        current = new ArrayList<>();
      }
    }
    return groups;
  }

  /**
   * Repeatedly settles the largest debtor against the largest creditor. Each payment zeroes at
   * least one balance, so a zero-sum group of size {@code s} needs at most {@code s - 1} payments.
   */
  private static List<Transfer> greedy(List<Balance> balances) {
    Comparator<Balance> largestFirst =
        Comparator.comparingLong((Balance b) -> Math.abs(b.cents()))
            .reversed()
            .thenComparingLong(Balance::memberId);
    PriorityQueue<Balance> creditors = new PriorityQueue<>(largestFirst);
    PriorityQueue<Balance> debtors = new PriorityQueue<>(largestFirst);
    for (Balance b : balances) {
      (b.cents() > 0 ? creditors : debtors).add(b);
    }

    List<Transfer> transfers = new ArrayList<>();
    while (!creditors.isEmpty() && !debtors.isEmpty()) {
      Balance creditor = creditors.poll();
      Balance debtor = debtors.poll();
      long amount = Math.min(creditor.cents(), -debtor.cents());
      transfers.add(new Transfer(debtor.memberId(), creditor.memberId(), amount));
      if (creditor.cents() > amount) {
        creditors.add(new Balance(creditor.memberId(), creditor.cents() - amount));
      }
      if (-debtor.cents() > amount) {
        debtors.add(new Balance(debtor.memberId(), debtor.cents() + amount));
      }
    }
    return transfers;
  }

  private record Balance(long memberId, long cents) {}
}
