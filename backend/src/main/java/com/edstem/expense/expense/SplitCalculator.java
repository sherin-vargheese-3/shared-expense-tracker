package com.edstem.expense.expense;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure functions for dividing an expense among participants. */
public final class SplitCalculator {

  private SplitCalculator() {}

  /**
   * Splits {@code amountCents} evenly across {@code memberIds}. When the amount does not divide
   * evenly, the remaining cents are handed out one each to members in ascending id order, so the
   * result is deterministic and the shares always sum to exactly {@code amountCents}.
   *
   * @return shares keyed by member id, in ascending id order
   */
  public static Map<Long, Long> equalSplit(long amountCents, Collection<Long> memberIds) {
    if (amountCents < 0) {
      throw new IllegalArgumentException("amountCents must not be negative");
    }
    if (memberIds.isEmpty()) {
      throw new IllegalArgumentException("memberIds must not be empty");
    }
    long[] ids = memberIds.stream().mapToLong(Long::longValue).sorted().distinct().toArray();
    if (ids.length != memberIds.size()) {
      throw new IllegalArgumentException("memberIds must be unique");
    }

    long base = amountCents / ids.length;
    long remainder = amountCents % ids.length;

    Map<Long, Long> shares = new LinkedHashMap<>();
    for (int i = 0; i < ids.length; i++) {
      shares.put(ids[i], base + (i < remainder ? 1 : 0));
    }
    return shares;
  }
}
