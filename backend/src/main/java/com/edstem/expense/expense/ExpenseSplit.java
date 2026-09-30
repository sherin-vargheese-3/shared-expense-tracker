package com.edstem.expense.expense;

import com.edstem.expense.group.Member;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_split")
public class ExpenseSplit {

  @EmbeddedId private ExpenseSplitId id = new ExpenseSplitId();

  @MapsId("expenseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "expense_id")
  private Expense expense;

  @MapsId("memberId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id")
  private Member member;

  @Column(name = "share_cents", nullable = false)
  private long shareCents;

  protected ExpenseSplit() {}

  ExpenseSplit(Expense expense, Member member, long shareCents) {
    this.expense = expense;
    this.member = member;
    this.shareCents = shareCents;
  }

  public Expense getExpense() {
    return expense;
  }

  public Member getMember() {
    return member;
  }

  public long getShareCents() {
    return shareCents;
  }
}
