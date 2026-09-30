package com.edstem.expense.balance;

/** Projection for per-member aggregate queries: a member id and a total in cents. */
public interface MemberAmount {

  Long getMemberId();

  Long getCents();
}
