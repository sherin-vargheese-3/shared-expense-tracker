package com.edstem.expense.expense;

public enum SplitType {
  /** Amount divided evenly; leftover cents go to the lowest member ids first. */
  EQUAL,
  /** Each participant's share is given explicitly and must sum to the expense amount. */
  EXACT
}
