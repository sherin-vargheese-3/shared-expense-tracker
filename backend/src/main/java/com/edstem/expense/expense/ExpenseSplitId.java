package com.edstem.expense.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ExpenseSplitId implements Serializable {

  @Column(name = "expense_id")
  private Long expenseId;

  @Column(name = "member_id")
  private Long memberId;

  protected ExpenseSplitId() {}

  ExpenseSplitId(Long expenseId, Long memberId) {
    this.expenseId = expenseId;
    this.memberId = memberId;
  }

  public Long getExpenseId() {
    return expenseId;
  }

  public Long getMemberId() {
    return memberId;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ExpenseSplitId other
        && Objects.equals(expenseId, other.expenseId)
        && Objects.equals(memberId, other.memberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expenseId, memberId);
  }
}
