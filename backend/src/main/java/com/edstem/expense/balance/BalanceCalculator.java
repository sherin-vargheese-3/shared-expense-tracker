package com.edstem.expense.balance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines per-member totals into net balances, in cents:
 *
 * <pre>net = paid - owed + settlementsSent - settlementsReceived</pre>
 *
 * Positive means the member is owed money; negative means they owe.
 */
public final class BalanceCalculator {

  private BalanceCalculator() {}

  /**
   * @param memberIds every member of the group; each gets an entry, even if zero
   * @return balances in the order of {@code memberIds}
   * @throws IllegalStateException if the balances do not sum to zero (data corruption)
   */
  public static Map<Long, Long> netBalances(
      Collection<Long> memberIds,
      List<MemberAmount> paid,
      List<MemberAmount> owed,
      List<MemberAmount> sent,
      List<MemberAmount> received) {
    Map<Long, Long> net = new LinkedHashMap<>();
    memberIds.forEach(id -> net.put(id, 0L));

    apply(net, paid, 1);
    apply(net, owed, -1);
    apply(net, sent, 1);
    apply(net, received, -1);

    long total = net.values().stream().mapToLong(Long::longValue).sum();
    if (total != 0) {
      throw new IllegalStateException("Balances sum to " + total + " cents instead of zero");
    }
    return net;
  }

  private static void apply(Map<Long, Long> net, List<MemberAmount> amounts, int sign) {
    for (MemberAmount amount : amounts) {
      if (!net.containsKey(amount.getMemberId())) {
        throw new IllegalStateException("Unknown member " + amount.getMemberId());
      }
      net.merge(amount.getMemberId(), sign * amount.getCents(), Math::addExact);
    }
  }
}
