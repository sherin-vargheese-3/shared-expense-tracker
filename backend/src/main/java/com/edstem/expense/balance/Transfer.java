package com.edstem.expense.balance;

/** A single suggested payment of {@code amountCents} from one member to another. */
public record Transfer(long fromMemberId, long toMemberId, long amountCents) {

  public Transfer {
    if (amountCents <= 0) {
      throw new IllegalArgumentException("amountCents must be positive");
    }
    if (fromMemberId == toMemberId) {
      throw new IllegalArgumentException("cannot transfer to self");
    }
  }
}
